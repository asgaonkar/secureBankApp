package com.asu.secureBankApp.service;

import java.util.Calendar;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.asu.secureBankApp.Config.Constants;
import com.asu.secureBankApp.Repository.AccountRepository;
import com.asu.secureBankApp.Repository.TransactionRepository;
import com.asu.secureBankApp.Repository.UserRepository;
import com.asu.secureBankApp.Request.TransferRequest;
import com.asu.secureBankApp.Request.UpdateBalanceRequest;
import com.asu.secureBankApp.Response.StatusResponse;
import com.asu.secureBankApp.dao.AccountDAO;
import com.asu.secureBankApp.dao.TransactionDAO;
import com.asu.secureBankApp.dao.UserDAO;
import com.asu.secureBankApp.util.Util;

import constants.ErrorCodes;
import constants.RoleType;
import constants.TransactionStatus;
import constants.TransactionType;

@Service
public class TransactionServiceImpl implements TransactionService {

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	UserRepository userRepository;
	
	@Autowired
	TransactionRepository transactionRepository;

	@Autowired
	private AccountService accountService;

	@Transactional
	public StatusResponse transfer(TransferRequest transferReq, Authentication auth, boolean isApproved) {
		StatusResponse response = new StatusResponse();
		response.setIsSuccess(false);

		AccountDAO fromAccount = accountRepository.findById(transferReq.getFromAccNo()).orElse(null);
		AccountDAO toAccount = accountRepository.findById(transferReq.getToAccNo()).orElse(null);

		if (fromAccount == null || toAccount == null) {
			response.setMsg(ErrorCodes.ID_NOT_FOUND);
			return response;
		}
		if(fromAccount.getBalance() - transferReq.getTransferAmount() < 0) {
			response.setMsg(ErrorCodes.INSUFFICIENT_FUNDS);
			return response;
		}
		RoleType accRoleType = fromAccount.getUser().getAuthRole().getRoleType();
		System.out.println("auth.getPrincipal()" + auth.getName());
		UserDAO user = userRepository.findByUsername(auth.getPrincipal().toString());
		RoleType authRoleType = user.getAuthRole()
				.getRoleType();
		if (authRoleType != accRoleType && !Util.isEmployee(authRoleType)) {
			response.setMsg(ErrorCodes.INVALID_ACCESS);
			return response;
		}
		UpdateBalanceRequest fromUpdateBalanceRequest = new UpdateBalanceRequest();
		UpdateBalanceRequest toUpdateBalanceRequest = new UpdateBalanceRequest();
		fromUpdateBalanceRequest.setAccountNo(fromAccount.getId());
		fromUpdateBalanceRequest.setAmount(-transferReq.getTransferAmount());
		
		toUpdateBalanceRequest.setAccountNo(toAccount.getId());
		toUpdateBalanceRequest.setAmount(transferReq.getTransferAmount());
		
		TransactionDAO transactionDAO = new TransactionDAO();
		
		boolean approvalRequired = !Util.isEmployee(authRoleType)
				&& transferReq.getTransferAmount() > Constants.TRANSFER_CRITICAL_LIMIT;
				
		if (approvalRequired) {
			transactionDAO.setCreatedBy(user);
			transactionDAO.setFromAccount(fromAccount);
			transactionDAO.setToAccount(toAccount);
			transactionDAO.setTransactionAmount(Math.abs(transferReq.getTransferAmount()));
			transactionDAO.setType(TransactionType.TRANSFER);
			transactionDAO.setStatus(TransactionStatus.PENDING);
			response = submitTransactionRequest(transactionDAO);
			if(response.getIsSuccess())
				response.setMsg(ErrorCodes.SUBMIT_APPROVAL);
		} else {
			if(doUpdateBalance(fromUpdateBalanceRequest) && doUpdateBalance(toUpdateBalanceRequest)) {
				if(isApproved) {
					response.setIsSuccess(true);
					return response;
				}
					
				transactionDAO.setCreatedBy(user);
				transactionDAO.setFromAccount(fromAccount);
				transactionDAO.setToAccount(toAccount);
				transactionDAO.setTransactionAmount(Math.abs(transferReq.getTransferAmount()));
				transactionDAO.setType(TransactionType.TRANSFER);
				transactionDAO.setStatus(TransactionStatus.COMPLETED);
				response = submitTransactionRequest(transactionDAO);
				if(response.getIsSuccess())
					response.setMsg(ErrorCodes.SUCCESS);				
			} else {
				throw new RuntimeException();
			}
		}
		return response;
	}

	@Override
	@Transactional
	public StatusResponse updateBalance(@Valid UpdateBalanceRequest updateBalanceRequest, Authentication auth, boolean isTransfer) {
		StatusResponse response = new StatusResponse();
		response.setIsSuccess(false);
		AccountDAO account = accountRepository.findById(updateBalanceRequest.getAccountNo()).orElse(null);
		if (account == null) {
			response.setMsg(ErrorCodes.ID_NOT_FOUND);
			return response;
		}
		if(account.getBalance() + updateBalanceRequest.getAmount() < 0) {
			response.setMsg(ErrorCodes.INSUFFICIENT_FUNDS);
			return response;
		}
		RoleType accRoleType = account.getUser().getAuthRole().getRoleType();
		System.out.println("auth.getPrincipal()" + auth.getName());
		UserDAO user = userRepository.findByUsername(auth.getPrincipal().toString());
		System.out.println("user.getAuthRole(): " + user.getAuthRole());
		RoleType authRoleType = user.getAuthRole()
				.getRoleType();
		if (!isTransfer && authRoleType != accRoleType && !Util.isEmployee(authRoleType)) {
			response.setMsg(ErrorCodes.INVALID_ACCESS);
			return response;
		}
		boolean approvalRequired = !Util.isEmployee(authRoleType)
				&& updateBalanceRequest.getAmount() > Constants.UPDATE_BALANCE_CRITICAL_LIMIT;
		TransactionDAO transactionDAO = new TransactionDAO();
		transactionDAO.setCreatedBy(user);
		transactionDAO.setFromAccount(account);
		transactionDAO.setTransactionAmount(Math.abs(updateBalanceRequest.getAmount()));
		transactionDAO.setType((updateBalanceRequest.getAmount()>0)?TransactionType.CREDIT : TransactionType.DEBIT);
		if (approvalRequired) {
			transactionDAO.setStatus(TransactionStatus.PENDING);
			response = submitTransactionRequest(transactionDAO);
			if(response.getIsSuccess())
				response.setMsg(ErrorCodes.SUBMIT_APPROVAL);
		} else {
			if(doUpdateBalance(updateBalanceRequest)) {
				transactionDAO.setStatus(TransactionStatus.COMPLETED);
				response = submitTransactionRequest(transactionDAO);
				if(response.getIsSuccess())
					response.setMsg(ErrorCodes.SUCCESS);			
			} else {
				throw new RuntimeException();
			}
		}
		return response;
	}

	@Override
	@Transactional
	public boolean doUpdateBalance(@Valid UpdateBalanceRequest updateBalanceRequest) {
		AccountDAO account = accountRepository.findById(updateBalanceRequest.getAccountNo()).orElse(null);
		if (account == null) {
			System.out.println(updateBalanceRequest.getAccountNo() + " does not exist");
			return false;
		}
		double bal = account.getBalance();
		bal += updateBalanceRequest.getAmount();
		account.setBalance(bal);
		accountRepository.save(account);
		return true;
	}
	
	@Override
	@Transactional
	public StatusResponse submitTransactionRequest(@Valid TransactionDAO transactionDAO) {
		transactionDAO.setTransactionTimestamp(Calendar.getInstance().getTime());
		transactionRepository.save(transactionDAO);
		// Insert to Hyperledger
		StatusResponse response = new StatusResponse();
		response.setIsSuccess(true);
		return response;
	}

	@Override
	public StatusResponse approveTransaction(String transactionId, Authentication auth) {
		StatusResponse response = new StatusResponse();
		response.setIsSuccess(false);
		UserDAO user = userRepository.findByUsername(auth.getPrincipal().toString());
		System.out.println("user.getAuthRole(): " + user.getAuthRole());
		RoleType authRoleType = user.getAuthRole()
				.getRoleType();
		if(!Util.isEmployee(authRoleType)) {
			response.setMsg(ErrorCodes.INVALID_ACCESS);
			return response; 
		}
		TransactionDAO transaction = transactionRepository.findById(Integer.parseInt(transactionId)).orElseGet(null);
		transaction.setApprovedAt(Calendar.getInstance().getTime());
		transaction.setApprovedBy(user);
		transaction.setStatus(TransactionStatus.APPROVED);
		transactionRepository.save(transaction);
		if(transaction.getType() == TransactionType.CREDIT || transaction.getType() == TransactionType.DEBIT) {
			UpdateBalanceRequest updateBalanceRequest = new UpdateBalanceRequest();
			updateBalanceRequest.setAccountNo(transaction.getFromAccount().getId());
			updateBalanceRequest.setAmount(transaction.getTransactionAmount());
			if(doUpdateBalance(updateBalanceRequest)) {
				response.setIsSuccess(true);
				response.setMsg(ErrorCodes.SUCCESS);
			} else {
				throw new RuntimeException();
			}
		} else {  // TRANSFER
			TransferRequest transferReq = new TransferRequest();
			transferReq.setFromAccNo(transaction.getFromAccount().getId());
			transferReq.setToAccNo(transaction.getToAccount().getId());
			transferReq.setTransferAmount(transaction.getTransactionAmount());
			response = transfer(transferReq, auth, true); 
			if(response.getIsSuccess()) {
				response.setIsSuccess(true);
				response.setMsg(ErrorCodes.SUCCESS);
			} else {
				throw new RuntimeException();
			}
		}
		return response;
	}


}
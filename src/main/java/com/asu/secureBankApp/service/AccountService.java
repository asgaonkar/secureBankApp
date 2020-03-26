package com.asu.secureBankApp.service;

import com.asu.secureBankApp.Request.UpdateInterestRequest;
import com.asu.secureBankApp.Response.AccountResponses;
import com.asu.secureBankApp.Response.StatusResponse;
import com.asu.secureBankApp.dao.CreateAccountReqDAO;

public interface AccountService {

	StatusResponse createAccount(CreateAccountReqDAO createAccountReqDAO);

	StatusResponse updateInterest(UpdateInterestRequest updateInterestRequest);
	
	AccountResponses getAccounts(String userId);

}

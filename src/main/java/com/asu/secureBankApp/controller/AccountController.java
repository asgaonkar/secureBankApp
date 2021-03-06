package com.asu.secureBankApp.controller;

import java.security.SecureRandom;
import java.util.HashMap;
import javax.validation.Valid;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import com.asu.secureBankApp.Repository.AccountRepository;
import com.asu.secureBankApp.Repository.UserRepository;
import com.asu.secureBankApp.Request.UpdateInterestRequest;
import com.asu.secureBankApp.Response.AccountResponses;
import com.asu.secureBankApp.Response.StatusResponse;
import com.asu.secureBankApp.dao.AccountDAO;
import com.asu.secureBankApp.dao.CreateAccountReqDAO;
import com.asu.secureBankApp.service.AccountService;

@Controller
@RequestMapping("/api/account")
public class AccountController {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	AccountService accountService;

	SecureRandom randomInt = new SecureRandom();

	@GetMapping(value = "/get/{user_id}")
	public @ResponseBody AccountResponses getAccounts(@PathVariable(value = "user_id") String userId, Authentication auth) {
		return accountService.getAccounts(userId);
	}
	
	@GetMapping(value = "/get")
	public @ResponseBody AccountResponses getAllAccounts() {
		return accountService.getAllAccounts();
	}
	
	@GetMapping(value = "/getbyEmail/{email_id:.+}")
	public @ResponseBody AccountResponses getAccountsbyEmail(@PathVariable(value = "email_id") String emailId) {
		return accountService.getAccountsbyEmail(emailId);
	}
	
	@GetMapping(value = "/getbyPhone/{phone}")
	public @ResponseBody AccountResponses getAccountsbyPhone(@PathVariable(value = "phone") String phone) {
		return accountService.getAccountsbyPhone(phone);
	}

	@PostMapping(value = "/createAccount", consumes = { "application/json" })
	public @ResponseBody StatusResponse createNewAccount(@RequestBody @Valid CreateAccountReqDAO createAccountReqDAO) {
		StatusResponse response = accountService.createAccount(createAccountReqDAO);
		return response;
	}

	@PostMapping(value = "/newAccount", consumes =  {"application/json"})
	public @ResponseBody HashMap<String, String> startNewAccount (@RequestBody AccountDAO account, Authentication authentication) throws JsonProcessingException {
		return accountService.createNewAccount(account, authentication);
	}

	@PatchMapping(value = "/updateInterest", consumes = { "application/json" })
	public @ResponseBody StatusResponse updateInterest(
			@RequestBody @Valid UpdateInterestRequest updateInterestRequest) {
		return accountService.updateInterest(updateInterestRequest);
	}
}

package com.asu.secureBankApp.service;

import java.util.Calendar;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.asu.secureBankApp.Config.Constants;
import com.asu.secureBankApp.Repository.AuthUserRepository;
import com.asu.secureBankApp.Repository.UserRepository;
import com.asu.secureBankApp.Request.LoginRequest;
import com.asu.secureBankApp.Request.LogoutRequest;
import com.asu.secureBankApp.Response.LoginResponse;
import com.asu.secureBankApp.dao.AuthUserDAO;
import com.asu.secureBankApp.dao.UserDAO;

import constants.Status;

@Service
public class LoginServiceImpl implements LoginService {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private AuthUserRepository authUserRepository;

	@Autowired
	private SystemLoggerService systemLoggerService;
	
	@Autowired
	PasswordEncoder encoder;
	
	@Transactional
	public LoginResponse login(@Valid LoginRequest loginRequest, Authentication auth) {
		LoginResponse response = new LoginResponse();
		response.setIsSuccess(false);
		UserDAO user = userRepository.findByUsername(auth.getName());
		if (user == null)
			return response;
		
		AuthUserDAO authUser = new AuthUserDAO();
		authUser.setUser(user);
		authUser.setStatus(Status.ACTIVE);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, 10);
		authUser.setExpiry(cal.getTime());
		authUserRepository.save(authUser);
		systemLoggerService.log(user.getId(), "User logged in", "TIME_OF_LOGIN");
		response.setIsSuccess(true);
		response.setUserId(user.getId());
		return response;
	}

	@Override
	@Transactional
	public LoginResponse logout(@Valid LogoutRequest logoutRequest) {
		LoginResponse response = new LoginResponse();
		UserDAO user = userRepository.findById(logoutRequest.getId()).orElse(null);
		if(user == null)
			return response;
		// List<AuthUserDAO> authUser = authUserRepository.findByUser(user);
		AuthUserDAO authUser = new AuthUserDAO();
		authUser.setUser(user);
		authUser.setStatus(Status.LOGGED_OUT);
		authUser.setExpiry(Calendar.getInstance().getTime());
		authUserRepository.save(authUser);
		
		response.setIsSuccess(true);
		response.setUserId(user.getId());
		return response;
	}
	
}

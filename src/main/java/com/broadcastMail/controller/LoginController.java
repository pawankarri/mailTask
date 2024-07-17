package com.broadcastMail.controller;

import com.broadcastMail.dto.LoginDto;
import com.broadcastMail.dto.PasswordDto;
import com.broadcastMail.service.UserMassMailService;
import com.broadcastMail.serviceImpl.JwtServiceImpl;
import com.broadcastMail.serviceImpl.UserSecurityDetailsImpl;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/auth")
public class LoginController {

    @Autowired
    private UserSecurityDetailsImpl userService;
    @Autowired
    private JwtServiceImpl service;
    @Autowired
    private UserMassMailService userMassMailService;
    @Autowired
    private AuthenticationManager authenticate;

    Map<String, Object> map = new HashMap<>();
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginDto loginDto) throws Exception {
        this.authenticate(loginDto.getMailId(), loginDto.getPassword());
        UserDetails userDetails = this.userService.loadUserByUsername(loginDto.getMailId());
        String token = this.service.generateToken(userDetails.getUsername());

        map.put("token", token);
        map.put("token expiration time", this.service.getExpirationDateFromToken(token).toString());
        map.put("token expiration time in milli seconds", this.service.getExpirationDateFromToken(token).getTime());
        map.put("message", "success");
        map.put("status", HttpStatus.OK.value());
        map.put("username", userDetails.getUsername());
        map.put("user-role", userDetails.getAuthorities().stream().map(auth -> auth.getAuthority()));

        return ResponseEntity.ok().body(map);

    }
    private void authenticate(String username,String password) throws Exception {
        UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(username,password);
        try {
            this.authenticate.authenticate(authenticationToken);
        }
        catch (DisabledException e)
        {
            throw new DisabledException("user is disabled");
        }
        catch (BadCredentialsException e)
        {
            throw new BadCredentialsException("bad credentials");
        }
    }


    @GetMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword()throws MessagingException, IOException {
        Map<String,Object> map=this.userMassMailService.forgotPassword();
        return ResponseEntity.ok().body(map);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String,Object>> resetPassword(@RequestBody PasswordDto passwordDto)
    {
        Map<String,Object> map=this.userMassMailService.resetPassword(passwordDto);
        return ResponseEntity.ok().body(map);
    }
}

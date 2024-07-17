package com.broadcastMail.serviceImpl;

import com.broadcastMail.entites.User;
import com.broadcastMail.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserSecurityDetailsImpl implements UserDetailsService {

    @Autowired
    private UserRepository repository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user=this.repository.findByMailId(username);
        if(user==null)
        {
            throw new UsernameNotFoundException("user doesnot exists");
        }
        return user;
    }

}

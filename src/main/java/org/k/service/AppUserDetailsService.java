package org.k.service;

import com.google.common.collect.ImmutableList;
import org.k.config.SecurityConfig;
import org.k.exception.ConfigException;
import org.k.user.UserInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Optional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final PropertiesService propertiesService;

    @Inject
    public AppUserDetailsService(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserInfo userInfo = propertiesService.getUserInfo(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        ImmutableList<GrantedAuthority> grantedAuthorities = ImmutableList.of(
                new SimpleGrantedAuthority(SecurityConfig.DEFAULT_ROLE_PREFIX + userInfo.getRole()));
        return new User(username, userInfo.getPasswordHash(), true, true, true, true, grantedAuthorities);
    }
}
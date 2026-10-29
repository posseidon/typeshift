package hu.elte.ntb.typeshift.service;

import hu.elte.ntb.typeshift.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads TypeShift accounts for the username/password form login flow.
 */
@Service
public class TypeShiftUserDetailsService implements UserDetailsService {

  private final UserAccountRepository accounts;

  public TypeShiftUserDetailsService(UserAccountRepository accounts) {
    this.accounts = accounts;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return accounts
        .findByUsername(username)
        .map(
            account ->
                User.withUsername(account.getUsername())
                    .password(account.getPasswordHash())
                    .roles("USER")
                    .build())
        .orElseThrow(() -> new UsernameNotFoundException("Unknown username: " + username));
  }
}

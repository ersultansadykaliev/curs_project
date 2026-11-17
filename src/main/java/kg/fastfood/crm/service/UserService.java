package kg.fastfood.crm.service;

import kg.fastfood.crm.dto.PasswordForm;
import kg.fastfood.crm.dto.UserForm;
import kg.fastfood.crm.entity.Role;
import kg.fastfood.crm.entity.User;
import kg.fastfood.crm.repository.RoleRepository;
import kg.fastfood.crm.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Управление пользователями системы и интеграция со Spring Security. */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()))
        );
    }

    @Transactional(readOnly = true)
    public List<User> listAll() {
        return userRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    @Transactional
    public void createUser(UserForm form) {
        String username = form.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Пользователь с логином «" + username + "» уже существует");
        }
        String roleName = Role.ADMIN.equalsIgnoreCase(form.getRole()) ? Role.ADMIN : Role.MANAGER;
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException("Роль не найдена: " + roleName));
        User user = new User();
        user.setUsername(username);
        user.setFullName(form.getFullName().trim());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void toggleEnabled(Long id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (user.getUsername().equals(currentUsername)) {
            throw new BusinessException("Нельзя заблокировать собственную учётную запись");
        }
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, PasswordForm form) {
        User user = findByUsername(username);
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Текущий пароль указан неверно");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Новый пароль и подтверждение не совпадают");
        }
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);
    }
}

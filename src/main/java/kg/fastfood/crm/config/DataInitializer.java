package kg.fastfood.crm.config;

import kg.fastfood.crm.entity.Role;
import kg.fastfood.crm.entity.User;
import kg.fastfood.crm.repository.OrderRepository;
import kg.fastfood.crm.repository.ProductRepository;
import kg.fastfood.crm.repository.RoleRepository;
import kg.fastfood.crm.repository.UserRepository;
import kg.fastfood.crm.service.DemoDataService;
import kg.fastfood.crm.service.SettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Инициализация при первом запуске: создаёт роли, учётную запись администратора
 * и при пустой базе загружает демонстрационный набор данных.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final SettingsService settingsService;
    private final DemoDataService demoDataService;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.demo.load-on-empty:true}")
    private boolean loadDemoOnEmpty;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           OrderRepository orderRepository,
                           ProductRepository productRepository,
                           PasswordEncoder passwordEncoder,
                           SettingsService settingsService,
                           DemoDataService demoDataService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.settingsService = settingsService;
        this.demoDataService = demoDataService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role adminRole = roleRepository.findByName(Role.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(Role.ADMIN, "Администратор системы")));
        roleRepository.findByName(Role.MANAGER)
                .orElseGet(() -> roleRepository.save(new Role(Role.MANAGER, "Руководитель / менеджер")));

        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setFullName("Администратор");
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(adminRole);
            admin.setEnabled(true);
            userRepository.save(admin);
        }

        if (settingsService.get(SettingsService.KEY_BUSINESS_NAME, null) == null) {
            settingsService.saveGeneral("Fast Food Bishkek", 9, 23);
        }

        boolean dbEmpty = orderRepository.count() == 0 && productRepository.count() == 0;
        if (loadDemoOnEmpty && dbEmpty) {
            demoDataService.loadIfEmpty();
        }
    }
}

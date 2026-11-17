package kg.fastfood.crm;

import kg.fastfood.crm.dto.AnalyticsResults.*;
import kg.fastfood.crm.dto.FinancialSummary;
import kg.fastfood.crm.dto.ImportResult;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:fastfood_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "app.demo.load-on-empty=true"
})
class IntegrationCrmTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private CsvImportService csvImportService;

    @Test
    void demoDatasetHasAround400OrdersAndAllModelsCalculateFromDatabase() {
        DemoDataService.DemoStats stats = demoDataService.stats();
        assertTrue(stats.orders() >= 350 && stats.orders() <= 460,
                "Ожидалось ~400 демо-заказов, фактически: " + stats.orders());
        assertTrue(stats.products() >= 10);
        assertTrue(stats.ingredients() >= 15);

        PeriodRange period = periodService.dataRange();
        SalesSnapshot snapshot = financeService.load(period);
        FinancialSummary summary = financeService.summary(snapshot);

        assertTrue(summary.revenue() > 0);
        assertTrue(summary.cogs() > 0);
        assertEquals(summary.revenue() - summary.cogs(), summary.grossProfit(), 1e-6);
        assertEquals(summary.grossProfit() - summary.operatingExpenses(), summary.operatingProfit(), 1e-6);
        // Закупка продуктов учитывается отдельно и НЕ включается в OE
        assertTrue(summary.foodPurchases() > 0);

        AverageResult avg = analyticsService.averages(snapshot);
        assertNull(avg.error());
        assertEquals(summary.averageCheck(), avg.averageCheck(), 1e-4);

        ProductivityResult prod = analyticsService.productivity(snapshot);
        assertNull(prod.error());
        assertTrue(prod.productivity() > 0);

        PoissonResult poisson = analyticsService.poisson(snapshot, AnalyticsService.INTERVAL_HOUR, null, null, prod.productivity());
        assertNull(poisson.error());
        assertTrue(poisson.lambda() > 0);
        assertNotNull(poisson.recommendedStaff());

        ForecastResult forecast = analyticsService.forecast(snapshot, 7);
        assertNull(forecast.error());
        assertNotNull(forecast.forecastValue());
        assertNotNull(forecast.mae());

        MarginResult margins = analyticsService.margins(snapshot);
        assertNull(margins.error());
        assertFalse(margins.rows().isEmpty());

        BreakEvenResult be = analyticsService.breakEven(snapshot, summary);
        assertNull(be.error());
        assertTrue(be.breakEvenUnits() > 0);
        assertTrue(be.breakEvenRevenue() > 0);
    }

    @Test
    void csvImportValidatesPreventsDuplicatesAndIsolatesRealFromDemo() {
        boolean prevMode = settingsService.isDemoMode();
        try {
            settingsService.setDemoMode(false);

            MockMultipartFile ingFile = new MockMultipartFile("file", "ingredients.csv", "text/csv",
                    ("name;unit;unit_cost\n"
                            + "Булочка тест;шт;20.00\n"
                            + "Булочка тест;шт;20.00\n"
                            + ";шт;10.00\n").getBytes(StandardCharsets.UTF_8));
            ImportResult ingRes = csvImportService.importFile("ingredients", ingFile);
            assertEquals(1, ingRes.getImported());
            assertEquals(1, ingRes.getDuplicates());
            assertTrue(ingRes.hasErrors());

            MockMultipartFile prodFile = new MockMultipartFile("file", "products.csv", "text/csv",
                    "name;category;price;active\nБургер тест;Бургеры;200.00;true\n".getBytes(StandardCharsets.UTF_8));
            ImportResult prodRes = csvImportService.importFile("products", prodFile);
            assertEquals(1, prodRes.getImported());

            MockMultipartFile recFile = new MockMultipartFile("file", "recipes.csv", "text/csv",
                    "product_name;ingredient_name;quantity\nБургер тест;Булочка тест;1.0\n".getBytes(StandardCharsets.UTF_8));
            ImportResult recRes = csvImportService.importFile("recipes", recFile);
            assertEquals(1, recRes.getImported());

            MockMultipartFile ordFile = new MockMultipartFile("file", "orders.csv", "text/csv",
                    ("order_number;date;time;items;total_amount;comment\n"
                            + "R-001;2026-11-10;12:30;\"Бургер тест x2\";;Тест\n"
                            + "R-001;2026-11-10;12:30;\"Бургер тест x2\";;Дубликат\n").getBytes(StandardCharsets.UTF_8));
            ImportResult ordRes = csvImportService.importFile("orders", ordFile);
            assertEquals(1, ordRes.getImported());
            assertEquals(1, ordRes.getDuplicates());

            // Очистка демо-данных не должна затронуть реальные данные
            demoDataService.clearDemoData();
            DemoDataService.DemoStats afterClear = demoDataService.stats();
            assertEquals(0, afterClear.orders());
            assertEquals(1, afterClear.realOrders());
            assertEquals(1, afterClear.realProducts());
            assertEquals(1, afterClear.realIngredients());

            // Восстанавливаем демо-набор для последующих проверок
            demoDataService.reloadDemoData();
        } finally {
            settingsService.setDemoMode(prevMode);
        }
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Autowired
    private kg.fastfood.crm.repository.OrderRepository orderRepository;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void allPagesRenderSuccessfullyForAuthenticatedAdmin() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/orders")).andExpect(status().isOk());
        mockMvc.perform(get("/orders/new")).andExpect(status().isOk());
        Long anyOrderId = orderRepository.findAll().stream().filter(o -> o.isDemo()).findFirst().orElseThrow().getId();
        mockMvc.perform(get("/orders/" + anyOrderId)).andExpect(status().isOk());
        mockMvc.perform(get("/orders/" + anyOrderId + "/edit")).andExpect(status().isOk());
        mockMvc.perform(get("/products")).andExpect(status().isOk());
        mockMvc.perform(get("/products/new")).andExpect(status().isOk());
        mockMvc.perform(get("/ingredients")).andExpect(status().isOk());
        mockMvc.perform(get("/ingredients/new")).andExpect(status().isOk());
        mockMvc.perform(get("/costing")).andExpect(status().isOk());
        mockMvc.perform(get("/expenses")).andExpect(status().isOk());
        mockMvc.perform(get("/expenses/new")).andExpect(status().isOk());
        mockMvc.perform(get("/staff")).andExpect(status().isOk());
        mockMvc.perform(get("/staff/employees/new")).andExpect(status().isOk());
        mockMvc.perform(get("/staff/shifts/new")).andExpect(status().isOk());
        mockMvc.perform(get("/analytics")).andExpect(status().isOk());
        mockMvc.perform(get("/finance")).andExpect(status().isOk());
        mockMvc.perform(get("/settings")).andExpect(status().isOk());
        mockMvc.perform(get("/settings/import/template/orders")).andExpect(status().isOk());
    }
}

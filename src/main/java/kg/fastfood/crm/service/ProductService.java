package kg.fastfood.crm.service;

import kg.fastfood.crm.dto.ProductForm;
import kg.fastfood.crm.dto.ProductRow;
import kg.fastfood.crm.dto.RecipeLineForm;
import kg.fastfood.crm.entity.Ingredient;
import kg.fastfood.crm.entity.Product;
import kg.fastfood.crm.entity.ProductIngredient;
import kg.fastfood.crm.math.CostCalculator;
import kg.fastfood.crm.math.CostCalculator.IngredientUsage;
import kg.fastfood.crm.repository.IngredientRepository;
import kg.fastfood.crm.repository.OrderRepository;
import kg.fastfood.crm.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Товары, их состав (технологические карты) и расчёт себестоимости C = Σ(qᵢ × pᵢ). */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;
    private final OrderRepository orderRepository;
    private final SettingsService settingsService;

    public ProductService(ProductRepository productRepository,
                          IngredientRepository ingredientRepository,
                          OrderRepository orderRepository,
                          SettingsService settingsService) {
        this.productRepository = productRepository;
        this.ingredientRepository = ingredientRepository;
        this.orderRepository = orderRepository;
        this.settingsService = settingsService;
    }

    /** Себестоимость одной порции по составу товара (состав должен быть загружен). */
    public static double costOf(Product product) {
        List<IngredientUsage> recipe = new ArrayList<>();
        for (ProductIngredient line : product.getIngredients()) {
            recipe.add(new IngredientUsage(line.getQuantity().doubleValue(),
                    line.getIngredient().getUnitCost().doubleValue()));
        }
        return CostCalculator.productCost(recipe);
    }

    /** Себестоимость всех товаров текущего набора данных: id товара → C. */
    @Transactional(readOnly = true)
    public Map<Long, Double> costMap() {
        Map<Long, Double> costs = new HashMap<>();
        for (Product p : productRepository.findAllWithIngredients(settingsService.isDemoMode())) {
            costs.put(p.getId(), costOf(p));
        }
        return costs;
    }

    /** Таблица товаров с себестоимостью, маржой и количеством продаж. */
    @Transactional(readOnly = true)
    public List<ProductRow> listRows() {
        boolean demo = settingsService.isDemoMode();
        Map<Long, Long> unitsSold = new HashMap<>();
        for (Object[] row : orderRepository.unitsSoldByProduct(demo)) {
            unitsSold.put((Long) row[0], ((Number) row[1]).longValue());
        }
        List<ProductRow> rows = new ArrayList<>();
        for (Product p : productRepository.findAllWithIngredients(demo)) {
            double price = p.getPrice().doubleValue();
            double cost = costOf(p);
            rows.add(new ProductRow(p.getId(), p.getName(), p.getCategory(), price, cost,
                    CostCalculator.unitMargin(price, cost), CostCalculator.marginPercent(price, cost),
                    unitsSold.getOrDefault(p.getId(), 0L), p.getIngredients().size(), p.isActive()));
        }
        return rows;
    }

    /** Товар вместе с составом. */
    @Transactional(readOnly = true)
    public Product getWithRecipe(Long id) {
        Product product = productRepository.findWithIngredientsById(id)
                .orElseThrow(() -> new NotFoundException("Товар не найден"));
        checkDataSet(product);
        product.getIngredients().sort(Comparator.comparing(pi -> pi.getIngredient().getName()));
        return product;
    }

    /** Активные товары для выбора в заказе. */
    @Transactional(readOnly = true)
    public List<Product> activeProducts() {
        return productRepository.findByDemoAndActiveTrueOrderByNameAsc(settingsService.isDemoMode());
    }

    @Transactional(readOnly = true)
    public ProductForm toForm(Long id) {
        Product p = getWithRecipe(id);
        ProductForm form = new ProductForm();
        form.setId(p.getId());
        form.setName(p.getName());
        form.setCategory(p.getCategory());
        form.setPrice(p.getPrice());
        form.setActive(p.isActive());
        return form;
    }

    /** Создаёт или обновляет товар. Возвращает id. */
    @Transactional
    public Long save(ProductForm form) {
        boolean demo = settingsService.isDemoMode();
        String name = form.getName().trim();
        Optional<Product> sameName = productRepository.findByNameIgnoreCaseAndDemo(name, demo);
        if (sameName.isPresent() && !sameName.get().getId().equals(form.getId())) {
            throw new BusinessException("Товар «" + name + "» уже существует");
        }
        Product product;
        if (form.getId() == null) {
            product = new Product();
            product.setDemo(demo);
        } else {
            product = productRepository.findById(form.getId())
                    .orElseThrow(() -> new NotFoundException("Товар не найден"));
            checkDataSet(product);
        }
        product.setName(name);
        product.setCategory(blankToNull(form.getCategory()));
        product.setPrice(form.getPrice());
        product.setActive(form.isActive());
        return productRepository.save(product).getId();
    }

    /** Удаляет товар. Товар, который уже продавался, удалить нельзя — его можно сделать неактивным. */
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Товар не найден"));
        checkDataSet(product);
        long usages = orderRepository.countItemsByProduct(id);
        if (usages > 0) {
            throw new BusinessException("Товар «" + product.getName() + "» встречается в " + usages
                    + " позициях заказов. Удаление исказит историю продаж — снимите отметку «Активен», чтобы убрать его из меню.");
        }
        productRepository.delete(product);
    }

    /** Добавляет ингредиент в состав (или меняет его количество, если он уже есть). */
    @Transactional
    public void addRecipeLine(Long productId, RecipeLineForm form) {
        Product product = getWithRecipe(productId);
        Ingredient ingredient = ingredientRepository.findById(form.getIngredientId())
                .orElseThrow(() -> new NotFoundException("Ингредиент не найден"));
        if (ingredient.isDemo() != product.isDemo()) {
            throw new BusinessException("Ингредиент относится к другому набору данных");
        }
        for (ProductIngredient line : product.getIngredients()) {
            if (line.getIngredient().getId().equals(ingredient.getId())) {
                line.setQuantity(form.getQuantity());
                return;
            }
        }
        ProductIngredient line = new ProductIngredient();
        line.setProduct(product);
        line.setIngredient(ingredient);
        line.setQuantity(form.getQuantity());
        product.getIngredients().add(line);
        productRepository.save(product);
    }

    @Transactional
    public void removeRecipeLine(Long productId, Long lineId) {
        Product product = getWithRecipe(productId);
        boolean removed = product.getIngredients().removeIf(line -> line.getId().equals(lineId));
        if (!removed) {
            throw new NotFoundException("Строка состава не найдена");
        }
        productRepository.save(product);
    }

    private void checkDataSet(Product product) {
        if (product.isDemo() != settingsService.isDemoMode()) {
            throw new NotFoundException("Товар не найден в текущем наборе данных");
        }
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

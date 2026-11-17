package kg.fastfood.crm.service;

import kg.fastfood.crm.dto.IngredientForm;
import kg.fastfood.crm.entity.Ingredient;
import kg.fastfood.crm.repository.IngredientRepository;
import kg.fastfood.crm.repository.ProductIngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Справочник ингредиентов. */
@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final SettingsService settingsService;

    public IngredientService(IngredientRepository ingredientRepository,
                             ProductIngredientRepository productIngredientRepository,
                             SettingsService settingsService) {
        this.ingredientRepository = ingredientRepository;
        this.productIngredientRepository = productIngredientRepository;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public List<Ingredient> list() {
        return ingredientRepository.findByDemoOrderByNameAsc(settingsService.isDemoMode());
    }

    /** Сколько товаров использует каждый ингредиент: id → количество. */
    @Transactional(readOnly = true)
    public Map<Long, Long> usageCounts() {
        Map<Long, Long> usage = new HashMap<>();
        for (Ingredient ingredient : list()) {
            usage.put(ingredient.getId(), productIngredientRepository.countByIngredientId(ingredient.getId()));
        }
        return usage;
    }

    @Transactional(readOnly = true)
    public IngredientForm toForm(Long id) {
        Ingredient ingredient = get(id);
        IngredientForm form = new IngredientForm();
        form.setId(ingredient.getId());
        form.setName(ingredient.getName());
        form.setUnit(ingredient.getUnit());
        form.setUnitCost(ingredient.getUnitCost());
        return form;
    }

    @Transactional
    public Long save(IngredientForm form) {
        boolean demo = settingsService.isDemoMode();
        String name = form.getName().trim();
        Optional<Ingredient> sameName = ingredientRepository.findByNameIgnoreCaseAndDemo(name, demo);
        if (sameName.isPresent() && !sameName.get().getId().equals(form.getId())) {
            throw new BusinessException("Ингредиент «" + name + "» уже существует");
        }
        Ingredient ingredient;
        if (form.getId() == null) {
            ingredient = new Ingredient();
            ingredient.setDemo(demo);
        } else {
            ingredient = get(form.getId());
        }
        ingredient.setName(name);
        ingredient.setUnit(form.getUnit().trim());
        ingredient.setUnitCost(form.getUnitCost());
        return ingredientRepository.save(ingredient).getId();
    }

    @Transactional
    public void delete(Long id) {
        Ingredient ingredient = get(id);
        long usages = productIngredientRepository.countByIngredientId(id);
        if (usages > 0) {
            throw new BusinessException("Нельзя удалить «" + ingredient.getName()
                    + "»: ингредиент входит в состав " + usages + " товар(ов). Сначала уберите его из состава.");
        }
        ingredientRepository.delete(ingredient);
    }

    private Ingredient get(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ингредиент не найден"));
        if (ingredient.isDemo() != settingsService.isDemoMode()) {
            throw new NotFoundException("Ингредиент не найден в текущем наборе данных");
        }
        return ingredient;
    }
}

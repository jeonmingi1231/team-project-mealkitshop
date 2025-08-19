package org.team.mealkitshop.dto.item;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.team.mealkitshop.common.Category;
import org.team.mealkitshop.common.FoodItem;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.domain.item.Item;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ItemFormDTO {

    private Long id;

    @NotBlank(message = "상품명은 필수 입력 값입니다.")
    private String itemNm;

    @NotNull(message = "가격은 필수 입력 값입니다.")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private Integer price;

    @NotBlank(message = "상품 설명은 필수 입력 값입니다.")
    private String itemDetail;

    @NotNull(message = "재고는 필수 입력 값입니다.")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    private Integer stockNumber;

    private ItemSellStatus itemSellStatus;

    private Category category;

    @NotNull(message = "음식 종류는 필수 선택 값입니다.")
    private FoodItem foodItem;

    private List<ItemImgDTO> itemImgDTOList = new ArrayList<>();
    private List<Long> itemImgIds = new ArrayList<>();

    private static final ModelMapper modelMapper = new ModelMapper();

    public Item createItem() {
        // DTO 내부에서도 동기화(안전장치)
        setFoodItem(this.foodItem);
        return modelMapper.map(this, Item.class);
    }

    /** foodItem 설정 시 category 자동 동기화 */
    public void setFoodItem(FoodItem foodItem) {
        this.foodItem = foodItem;
        this.category = (foodItem != null) ? foodItem.getCategory() : null;
    }

    public static ItemFormDTO of(Item item) {
        ItemFormDTO dto = modelMapper.map(item, ItemFormDTO.class);
        // 역매핑 후에도 동기화(보수적)
        dto.setFoodItem(dto.getFoodItem());
        return dto;
    }
}

// Thymeleaf HTML 폼 예시
// <form th:action="@{/item/save}" th:object="${itemFormDTO}" method="post">
//
//  <!-- 중분류 select -->
//  <label for="foodItem">중분류 (Food Item):</label>
//  <select id="foodItem" name="foodItem" th:field="*{foodItem}">
//    <option th:each="fi : ${T(org.team.mealkitshop.common.FoodItem).values()}"
//            th:value="${fi}"
//            th:text="${fi.name()}">
//    </option>
//  </select>
//
//  <!-- 대분류 표시 -->
//  <label for="category">대분류 (Category):</label>
//  <input type="text" id="category" name="category" th:field="*{category}" readonly/>
//
//  <button type="submit">저장</button>
// </form>


//  JavaScript 예시
//<script>
//  // FoodItem → Category 매핑 객체 (enum 구조에 맞춰 작성)
//  const foodItemToCategory = {
//    'SET': 'SET',
//    'SALAD': 'REFRIGERATED',
//    'POKE': 'REFRIGERATED',
//    'CHICKEN_BREAST': 'FROZEN',
//    'FRIED_RICE': 'FROZEN',
//    'PROTEIN_DRINK': 'ETC',
//    'DRESSING': 'ETC'
//  };
//
//  const foodItemSelect = document.getElementById('foodItem');
//  const categoryInput = document.getElementById('category');
//
//  function updateCategory() {
//    const selectedFoodItem = foodItemSelect.value;
//    const mappedCategory = foodItemToCategory[selectedFoodItem] || '';
//    categoryInput.value = mappedCategory;
//  }
//
//  // 초기 로딩 시 대분류 표시 (초기값 설정)
//  updateCategory();
//
//  // 중분류 변경 이벤트 감지
//  foodItemSelect.addEventListener('change', updateCategory);
//</script>

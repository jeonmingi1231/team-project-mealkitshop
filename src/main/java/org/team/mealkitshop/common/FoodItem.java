package org.team.mealkitshop.common;

public enum FoodItem {
    SET (Category.SET),
    SALAD(Category.REFRIGERATED),
    POKE(Category.REFRIGERATED),
    CHICKEN_BREAST(Category.FROZEN),
    FRIED_RICE(Category.FROZEN),
    PROTEIN_DRINK(Category.ETC),
    DRESSING(Category.ETC);

    private final Category category;

    FoodItem(Category category) {
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }
}

ALTER TABLE ChatShop_listings ADD COLUMN enchantments VARCHAR(30) AFTER price;
ALTER TABLE ChatShop_transactions ADD COLUMN enchantments VARCHAR(30) AFTER price;
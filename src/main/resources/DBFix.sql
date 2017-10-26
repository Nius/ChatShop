ALTER TABLE ChatShop_listings ADD COLUMN itemName TEXT AFTER enchantments;
ALTER TABLE ChatShop_transactions ADD COLUMN itemName TEXT AFTER enchantments;

-- If upgrading from pre-2.0.0
ALTER TABLE ChatShop_listings ADD COLUMN enchantments VARCHAR(30) AFTER price;
ALTER TABLE ChatShop_transactions ADD COLUMN enchantments VARCHAR(30) AFTER price;
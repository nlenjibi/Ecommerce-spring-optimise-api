-- V1__add_performance_indexes.sql
-- Performance optimization indexes for Smart E-Commerce API
-- Created: March 2026
-- Purpose: Address VisualVM JDBC profiling showing slow queries

-- ============================================
-- Composite index for product listing queries
-- Addresses: 11.6ms query (50.3% of query time)
-- Used by: Product listing, sorting by sales/rating
-- ============================================
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_listing 
ON products(is_active, sales_count DESC, rating_average DESC, created_at DESC);

-- ============================================
-- Partial index for discounted products
-- Addresses: 3.44ms query (14.9% of query time)
-- Used by: getDiscountedProducts()
-- ============================================
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_discount 
ON products(is_active, discount_price, price) 
WHERE discount_price IS NOT NULL 
  AND discount_price > 0 
  AND discount_price < price;

-- ============================================
-- Partial index for in-stock products
-- Addresses: Inventory status queries
-- ============================================
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_inventory 
ON products(is_active, inventory_status, stock_quantity) 
WHERE is_active = true;

-- ============================================
-- Index for category-based queries
-- ============================================
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_category_active 
ON products(category_id, is_active);

-- ============================================
-- Composite index for search queries
-- Addresses: Product search by name/SKU
-- ============================================
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_search 
ON products(is_active, name, sku) 
WHERE is_active = true;

-- ============================================
-- Index for trending products
-- ============================================
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_trending 
ON products(is_active, sales_count DESC, rating_average DESC, view_count DESC)
WHERE is_active = true;

-- ============================================
-- Index for featured products
-- ============================================
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_featured 
ON products(is_active, featured, created_at DESC)
WHERE is_active = true AND featured = true;

-- ============================================
-- Analyze tables after index creation
-- ============================================
ANALYZE products;

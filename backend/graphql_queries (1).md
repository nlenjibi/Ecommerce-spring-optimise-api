# ============================================
# PRODUCT QUERIES
# ============================================

# 1. Get single product by ID
query GetProduct {
  product(id: 1) {
    id
    name
    description
    slug
    sku
    price
    discountPrice
    effectivePrice
    stockQuantity
    inventoryStatus
    imageUrl
    featured
    isNew
    category {
      id
      name
      slug
    }
    averageRating
    totalReviews
    isActive
    createdAt
    updatedAt
  }
}

# 2. Get product by slug
query GetProductBySlug {
  productBySlug(slug: "laptop-pro-2024") {
    id
    name
    price
    stockQuantity
    category {
      name
    }
  }
}

# 3. Get all products with pagination
query GetAllProducts {
  products(
    pagination: {
      page: 0
      size: 20
      sortBy: "createdAt"
      direction: DESC
    }
  ) {
    content {
      id
      name
      price
      stockQuantity
      featured
    }
    pageInfo {
      totalElements
      totalPages
      pageNumber
      pageSize
      hasNext
      hasPrevious
    }
  }
}

# 4. Get products with filters
query GetFilteredProducts {
  products(
    pagination: {
      page: 0
      size: 10
      sortBy: "price"
      direction: ASC
    }
    filter: {
      categoryId: 1
      minPrice: 100
      maxPrice: 5000
      search: "laptop"
      featured: true
      inStock: true
    }
  ) {
    content {
      id
      name
      price
      stockQuantity
    }
    pageInfo {
      totalElements
    }
  }
}

# 5. Get featured products
query GetFeaturedProducts {
  featuredProducts(
    pagination: {
      page: 0
      size: 6
    }
  ) {
    content {
      id
      name
      price
      imageUrl
      featured
    }
  }
}

# 6. Get products by category
query GetProductsByCategory {
  productsByCategory(
    categoryId: 3
    pagination: {
      page: 0
      size: 15
    }
  ) {
    content {
      id
      name
      price
      category {
        name
      }
    }
  }
}

# 7. Search products
query SearchProducts {
  searchProducts(
    search: "gaming"
    pagination: {
      page: 0
      size: 10
    }
  ) {
    content {
      id
      name
      price
      description
    }
  }
}

# ============================================
# CATEGORY QUERIES
# ============================================

# 8. Get single category
query GetCategory {
  category(id: 1) {
    id
    name
    slug
    description
    imageUrl
    displayOrder
    parent {
      id
      name
    }
    children {
      id
      name
      slug
    }
    level
    isActive
    createdAt
  }
}

# 9. Get category by slug
query GetCategoryBySlug {
  categoryBySlug(slug: "electronics") {
    id
    name
    description
    children {
      id
      name
    }
  }
}

# 10. Get all categories with pagination
query GetAllCategories {
  categories(
    pagination: {
      page: 0
      size: 20
      sortBy: "displayOrder"
      direction: ASC
    }
    isActive: true
  ) {
    content {
      id
      name
      slug
      level
      isActive
    }
    pageInfo {
      totalElements
    }
  }
}

# 11. Get active categories only
query GetActiveCategories {
  activeCategories {
    id
    name
    slug
    displayOrder
  }
}

# 12. Get root categories
query GetRootCategories {
  rootCategories(includeChildren: true) {
    id
    name
    slug
    children {
      id
      name
      children {
        id
        name
      }
    }
  }
}

# 13. Get full category hierarchy
query GetCategoryHierarchy {
  categoryHierarchy {
    id
    name
    level
    children {
      id
      name
      level
      children {
        id
        name
        level
      }
    }
  }
}

# ============================================
# CART QUERIES
# ============================================

# 14. Get cart
query GetCart {
  cart(id: "123e4567-e89b-12d3-a456-426614174000") {
    id
    status
    items {
      id
      product {
        id
        name
        price
      }
      quantity
      unitPrice
      totalPrice
    }
    totalPrice
    finalPrice
    itemCount
    dateCreated
    updatedAt
  }
}

# ============================================
# ORDER QUERIES
# ============================================

# 15. Get single order
query GetOrder {
  order(id: 1) {
    id
    orderNumber
    user {
      id
      email
      fullName
    }
    status
    orderDate
    subtotal
    totalAmount
    paymentStatus
    createdAt
  }
}

# 16. Get order by order number
query GetOrderByNumber {
  orderByNumber(orderNumber: "ORD-2024-001") {
    id
    orderNumber
    status
    totalAmount
    paymentStatus
  }
}

# 17. Get my orders
query GetMyOrders {
  myOrders(
    pagination: {
      page: 0
      size: 10
      sortBy: "orderDate"
      direction: DESC
    }
  ) {
    content {
      id
      orderNumber
      status
      orderDate
      totalAmount
      paymentStatus
    }
    pageInfo {
      totalElements
      hasNext
    }
  }
}

# 18. Get all orders (admin)
query GetAllOrders {
  allOrders(
    pagination: {
      page: 0
      size: 20
    }
  ) {
    content {
      id
      orderNumber
      user {
        email
      }
      status
      totalAmount
    }
  }
}

# 19. Get order statistics
query GetOrderStatistics {
  orderStatistics {
    totalOrders
    totalRevenue
    averageOrderValue
  }
}

# ============================================
# REVIEW QUERIES
# ============================================

# 20. Get product reviews
query GetProductReviews {
  productReviews(
    productId: 1
    pagination: {
      page: 0
      size: 10
      sortBy: "createdAt"
      direction: DESC
    }
  ) {
    content {
      id
      product {
        id
        name
      }
      user {
        id
        username
      }
      rating
      title
      comment
      verifiedPurchase
      createdAt
    }
    pageInfo {
      totalElements
    }
  }
}

# 21. Get product rating statistics
query GetProductRatingStats {
  productRatingStats(productId: 1) {
    productId
    averageRating
    totalReviews
  }
}

# ============================================
# USER QUERIES
# ============================================

# 22. Get single user
query GetUser {
  user(id: 1) {
    id
    email
    username
    firstName
    lastName
    fullName
    phoneNumber
    role
    isActive
    createdAt
    updatedAt
  }
}

# 23. Get all users
query GetAllUsers {
  users(
    pagination: {
      page: 0
      size: 20
      sortBy: "createdAt"
      direction: DESC
    }
  ) {
    content {
      id
      email
      username
      fullName
      role
      isActive
    }
    pageInfo {
      totalElements
    }
  }
}

# 24. Get current user
query GetCurrentUser {
  currentUser {
    id
    email
    username
    fullName
    role
  }
}

# 25. Get admin dashboard
query GetAdminDashboard {
  adminDashboard {
    totalUsers
    totalProducts
    totalOrders
    totalRevenue
    pendingOrders
    lowStockProducts
  }
}

# ============================================
# WISHLIST QUERIES
# ============================================

# 26. Get my wishlist
query GetMyWishlist {
  myWishlist {
    id
    user {
      id
      username
    }
    product {
      id
      name
      price
      imageUrl
    }
    priority
    createdAt
  }
}

# 27. Get paginated wishlist
query GetMyWishlistPaginated {
  myWishlistPaginated(
    pagination: {
      page: 0
      size: 10
      sortBy: "createdAt"
      direction: DESC
    }
  ) {
    content {
      id
      product {
        id
        name
        price
      }
      priority
    }
    pageInfo {
      totalElements
    }
  }
}

# 28. Get wishlist summary
query GetWishlistSummary {
  wishlistSummary {
    totalItems
    totalValue
  }
}

# 29. Check if product is in wishlist
query IsInWishlist {
  isInWishlist(productId: 5)
}

# 30. Get wishlist items with price drops
query GetWishlistItemsWithPriceDrops {
  wishlistItemsWithPriceDrops {
    id
    product {
      id
      name
      price
      discountPrice
    }
    priority
  }
}

# ============================================
# COMPLEX NESTED QUERIES
# ============================================

# 31. Complete product details with reviews and category
query GetCompleteProductDetails {
  product(id: 1) {
    id
    name
    description
    price
    discountPrice
    effectivePrice
    stockQuantity
    inventoryStatus
    imageUrl
    category {
      id
      name
      slug
      parent {
        id
        name
      }
    }
    averageRating
    totalReviews
  }
  
  productReviews(productId: 1, pagination: { page: 0, size: 5 }) {
    content {
      id
      rating
      title
      comment
      user {
        username
      }
      createdAt
    }
  }
  
  productRatingStats(productId: 1) {
    averageRating
    totalReviews
  }
}

# 32. Dashboard overview query
query GetDashboardOverview {
  adminDashboard {
    totalUsers
    totalProducts
    totalOrders
    totalRevenue
    pendingOrders
    lowStockProducts
  }
  
  orderStatistics {
    totalOrders
    totalRevenue
    averageOrderValue
  }
  
  featuredProducts(pagination: { page: 0, size: 4 }) {
    content {
      id
      name
      price
      imageUrl
    }
  }
}

# 33. User profile with orders and wishlist
query GetUserProfile {
  currentUser {
    id
    email
    username
    fullName
    role
  }
  
  myOrders(pagination: { page: 0, size: 5 }) {
    content {
      id
      orderNumber
      status
      totalAmount
      orderDate
    }
  }
  
  wishlistSummary {
    totalItems
    totalValue
  }
}

# 34. Category with products
query GetCategoryWithProducts {
  category(id: 1) {
    id
    name
    slug
    description
  }
  
  productsByCategory(categoryId: 1, pagination: { page: 0, size: 10 }) {
    content {
      id
      name
      price
      stockQuantity
      imageUrl
    }
    pageInfo {
      totalElements
    }
  }
}

# ============================================
# MUTATION EXAMPLES
# ============================================

# 35. Create product
mutation CreateProduct {
  createProduct(
    input: {
      name: "Gaming Laptop Pro"
      slug: "gaming-laptop-pro"
      price: 1499.99
      stockQuantity: 50
      categoryId: 1
      description: "High-performance gaming laptop"
      sku: "GLP-2024-001"
      imageUrl: "https://example.com/laptop.jpg"
      featured: true
      isNew: true
    }
  ) {
    id
    name
    price
    slug
  }
}

# 36. Update product
mutation UpdateProduct {
  updateProduct(
    id: 1
    input: {
      price: 1399.99
      stockQuantity: 45
      discountPrice: 1299.99
    }
  ) {
    id
    name
    price
    discountPrice
  }
}

# 37. Create category
mutation CreateCategory {
  createCategory(
    input: {
      name: "Gaming Accessories"
      slug: "gaming-accessories"
      description: "Everything for gamers"
      displayOrder: 10
      parentId: 1
    }
  ) {
    id
    name
    slug
  }
}

# 38. Create cart and add items
mutation CreateCartAndAddItem {
  createCart {
    id
    status
  }
}

mutation AddItemToCart {
  addItemToCart(
    cartId: "123e4567-e89b-12d3-a456-426614174000"
    input: {
      productId: 1
      quantity: 2
    }
  ) {
    id
    product {
      name
    }
    quantity
    totalPrice
  }
}

# 39. Create order
mutation CreateOrder {
  createOrder(
    input: {
      items: [
        { productId: 1, quantity: 2 }
        { productId: 3, quantity: 1 }
      ]
      shippingCost: 15.00
      paymentMethod: CREDIT_CARD
    }
  ) {
    id
    orderNumber
    status
    totalAmount
  }
}

# 40. Create review
mutation CreateReview {
  createReview(
    input: {
      productId: 1
      rating: 5
      title: "Excellent product!"
      comment: "Very satisfied with this purchase. Highly recommended!"
    }
  ) {
    id
    rating
    title
    comment
  }
}

# 41. Add to wishlist
mutation AddToWishlist {
  addToWishlist(
    input: {
      productId: 5
      priority: HIGH
    }
  ) {
    id
    product {
      name
    }
    priority
  }
}

# 42. Cancel order
mutation CancelOrder {
  cancelOrder(
    id: 1
    reason: "Changed my mind"
  ) {
    id
    status
  }
}

# 43. Ship order
mutation ShipOrder {
  shipOrder(
    id: 1
    trackingNumber: "TRACK123456"
    carrier: "DHL"
  ) {
    id
    status
  }
}

# 44. Delete product
mutation DeleteProduct {
  deleteProduct(id: 1)
}

# 45. Clear cart
mutation ClearCart {
  clearCart(cartId: "123e4567-e89b-12d3-a456-426614174000")
}

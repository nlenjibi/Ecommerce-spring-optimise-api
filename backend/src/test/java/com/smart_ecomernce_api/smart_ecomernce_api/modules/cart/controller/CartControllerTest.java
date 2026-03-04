package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.controller;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.repository.CartRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        productRepository.deleteAll();

        testProduct = Product.builder()
                .name("Test Product")
                .slug("test-product")
                .description("A test product")
                .price(new BigDecimal("99.99"))
                .stockQuantity(100)
                .sku("TEST-001")
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @Nested
    @DisplayName("POST /api/v1/carts - Create Cart")
    class CreateCart {

        @Test
        @DisplayName("Should create new cart successfully")
        void shouldCreateCart() throws Exception {
            mockMvc.perform(post("/api/v1/carts")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/carts/{id} - Get Cart")
    class GetCart {

        @Test
        @DisplayName("Should return cart by ID")
        void shouldReturnCartById() throws Exception {
            // First create a cart
            var cartResult = mockMvc.perform(post("/api/v1/carts"))
                    .andExpect(status().isCreated())
                    .andReturn();
            
            String response = cartResult.getResponse().getContentAsString();
            Long cartId = extractCartId(response);

            mockMvc.perform(get("/api/v1/carts/{id}", cartId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("Should return 404 for non-existent cart")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/api/v1/carts/{id}", 99999))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/carts/{cartId}/items - Add Item to Cart")
    class AddItemToCart {

        @Test
        @DisplayName("Should add item to cart successfully")
        void shouldAddItemToCart() throws Exception {
            // First create a cart
            var cartResult = mockMvc.perform(post("/api/v1/carts"))
                    .andExpect(status().isCreated())
                    .andReturn();
            
            String response = cartResult.getResponse().getContentAsString();
            Long cartId = extractCartId(response);

            String requestBody = """
                {
                    "productId": %d,
                    "quantity": 2
                }
                """.formatted(testProduct.getId());

            mockMvc.perform(post("/api/v1/carts/{cartId}/items", cartId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.quantity").value(2));
        }

        @Test
        @DisplayName("Should fail to add non-existent product")
        void shouldFailWithNonExistentProduct() throws Exception {
            var cartResult = mockMvc.perform(post("/api/v1/carts"))
                    .andExpect(status().isCreated())
                    .andReturn();
            
            String response = cartResult.getResponse().getContentAsString();
            Long cartId = extractCartId(response);

            String requestBody = """
                {
                    "productId": 99999,
                    "quantity": 1
                }
                """;

            mockMvc.perform(post("/api/v1/carts/{cartId}/items", cartId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should update quantity if product already in cart")
        void shouldUpdateQuantityIfExists() throws Exception {
            // First create cart and add item
            var cartResult = mockMvc.perform(post("/api/v1/carts"))
                    .andExpect(status().isCreated())
                    .andReturn();
            Long cartId = extractCartId(cartResult.getResponse().getContentAsString());

            String requestBody1 = """
                {
                    "productId": %d,
                    "quantity": 1
                }
                """.formatted(testProduct.getId());
            mockMvc.perform(post("/api/v1/carts/{cartId}/items", cartId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody1))
                    .andExpect(status().isCreated());

            // Add same product again
            String requestBody2 = """
                {
                    "productId": %d,
                    "quantity": 2
                }
                """.formatted(testProduct.getId());
            mockMvc.perform(post("/api/v1/carts/{cartId}/items", cartId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody2))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/carts/{cartId}/items/{productId} - Update Item Quantity")
    class UpdateItemQuantity {

        @Test
        @DisplayName("Should update item quantity")
        void shouldUpdateQuantity() throws Exception {
            // Create cart and add item
            var cartResult = mockMvc.perform(post("/api/v1/carts"))
                    .andExpect(status().isCreated())
                    .andReturn();
            Long cartId = extractCartId(cartResult.getResponse().getContentAsString());

            String addRequest = """
                {
                    "productId": %d,
                    "quantity": 2
                }
                """.formatted(testProduct.getId());
            mockMvc.perform(post("/api/v1/carts/{cartId}/items", cartId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addRequest))
                    .andExpect(status().isCreated());

            // Update quantity
            String updateRequest = """
                {
                    "quantity": 5
                }
                """;
            mockMvc.perform(put("/api/v1/carts/{cartId}/items/{productId}", cartId, testProduct.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should remove item when quantity is set to 0")
        void shouldRemoveItemWhenZero() throws Exception {
            // Create cart and add item
            var cartResult = mockMvc.perform(post("/api/v1/carts"))
                    .andExpect(status().isCreated())
                    .andReturn();
            Long cartId = extractCartId(cartResult.getResponse().getContentAsString());

            String addRequest = """
                {
                    "productId": %d,
                    "quantity": 2
                }
                """.formatted(testProduct.getId());
            mockMvc.perform(post("/api/v1/carts/{cartId}/items", cartId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addRequest))
                    .andExpect(status().isCreated());

            // Update to 0 to remove
            String updateRequest = """
                {
                    "quantity": 0
                }
                """;
            mockMvc.perform(put("/api/v1/carts/{cartId}/items/{productId}", cartId, testProduct.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/carts/{cartId}/items/{productId} - Remove Item")
    class RemoveItem {

        @Test
        @DisplayName("Should remove item from cart")
        void shouldRemoveItem() throws Exception {
            // Create cart and add item
            var cartResult = mockMvc.perform(post("/api/v1/carts"))
                    .andExpect(status().isCreated())
                    .andReturn();
            Long cartId = extractCartId(cartResult.getResponse().getContentAsString());

            String addRequest = """
                {
                    "productId": %d,
                    "quantity": 2
                }
                """.formatted(testProduct.getId());
            mockMvc.perform(post("/api/v1/carts/{cartId}/items", cartId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addRequest))
                    .andExpect(status().isCreated());

            // Remove item
            mockMvc.perform(delete("/api/v1/carts/{cartId}/items/{productId}", cartId, testProduct.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/carts/{cartId}/items - Clear Cart")
    class ClearCart {

        @Test
        @DisplayName("Should clear all items from cart")
        void shouldClearCart() throws Exception {
            // Create cart and add items
            var cartResult = mockMvc.perform(post("/api/v1/carts"))
                    .andExpect(status().isCreated())
                    .andReturn();
            Long cartId = extractCartId(cartResult.getResponse().getContentAsString());

            String addRequest = """
                {
                    "productId": %d,
                    "quantity": 2
                }
                """.formatted(testProduct.getId());
            mockMvc.perform(post("/api/v1/carts/{cartId}/items", cartId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addRequest))
                    .andExpect(status().isCreated());

            // Clear cart
            mockMvc.perform(delete("/api/v1/carts/{cartId}/items", cartId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // Helper method to extract cart ID from JSON response
    private Long extractCartId(String jsonResponse) {
        try {
            // Simple extraction - looking for "id":number pattern
            String[] parts = jsonResponse.split("\"id\":");
            if (parts.length > 1) {
                String idPart = parts[1].split(",")[0].trim();
                return Long.parseLong(idPart);
            }
        } catch (Exception e) {
            // Fallback
        }
        return 1L;
    }
}

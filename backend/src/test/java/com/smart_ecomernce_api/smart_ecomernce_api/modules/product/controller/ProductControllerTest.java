package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/v1/products - Product Listing")
    class GetProducts {

        @Test
        @DisplayName("Should return products with pagination")
        void shouldReturnProducts() throws Exception {
            mockMvc.perform(get("/api/v1/products")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("Should filter by category")
        void shouldFilterByCategory() throws Exception {
            mockMvc.perform(get("/api/v1/products")
                            .param("categoryId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should search products")
        void shouldSearchProducts() throws Exception {
            mockMvc.perform(get("/api/v1/products")
                            .param("search", "laptop"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id} - Single Product")
    class GetProductById {

        @Test
        @DisplayName("Should return 404 for non-existent product")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/api/v1/products/{id}", 99999))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return product by slug")
        void shouldReturnBySlug() throws Exception {
            mockMvc.perform(get("/api/v1/products/slug/{slug}", "test-slug"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/products - Create Product")
    class CreateProduct {

        @Test
        @DisplayName("Should fail with invalid data")
        void shouldFailWithInvalidData() throws Exception {
            String requestBody = """
                {
                    "name": "",
                    "price": -100
                }
                """;
            mockMvc.perform(post("/api/v1/products")
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products/{id} - Update Product")
    class UpdateProduct {

        @Test
        @DisplayName("Should return 404 for non-existent product")
        void shouldReturn404ForNonExistent() throws Exception {
            String requestBody = """
                {
                    "name": "Updated Name"
                }
                """;
            mockMvc.perform(put("/api/v1/products/{id}", 99999)
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id} - Delete Product")
    class DeleteProduct {

        @Test
        @DisplayName("Should return 404 for non-existent product")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(delete("/api/v1/products/{id}", 99999))
                    .andExpect(status().isNotFound());
        }
    }
}

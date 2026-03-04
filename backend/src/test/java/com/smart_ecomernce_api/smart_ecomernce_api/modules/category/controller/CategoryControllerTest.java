package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.controller;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.CategoryRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();

        testCategory = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices and accessories")
                .displayOrder(1)
                .isActive(true)
                .featured(true)
                .build();
        testCategory = categoryRepository.save(testCategory);
    }

    @Nested
    @DisplayName("GET /api/v1/categories - Category Listing")
    class GetCategories {

        @Test
        @DisplayName("Should return all categories with pagination")
        void shouldReturnAllCategories() throws Exception {
            mockMvc.perform(get("/api/v1/categories")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.data.content[0].name").value("Electronics"));
        }

        @Test
        @DisplayName("Should filter active categories only")
        void shouldFilterActiveCategories() throws Exception {
            mockMvc.perform(get("/api/v1/categories")
                            .param("active", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].isActive").value(true));
        }

        @Test
        @DisplayName("Should return featured categories only")
        void shouldReturnFeaturedCategories() throws Exception {
            mockMvc.perform(get("/api/v1/categories")
                            .param("featured", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].featured").value(true));
        }

        @Test
        @DisplayName("Should search categories by name")
        void shouldSearchCategories() throws Exception {
            mockMvc.perform(get("/api/v1/categories")
                            .param("search", "Electronics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].name").value("Electronics"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/{id} - Single Category")
    class GetCategoryById {

        @Test
        @DisplayName("Should return category by ID")
        void shouldReturnCategoryById() throws Exception {
            mockMvc.perform(get("/api/v1/categories/{id}", testCategory.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Electronics"))
                    .andExpect(jsonPath("$.data.slug").value("electronics"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent category")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/api/v1/categories/{id}", 99999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return category by slug")
        void shouldReturnCategoryBySlug() throws Exception {
            mockMvc.perform(get("/api/v1/categories/slug/{slug}", "electronics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Electronics"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/categories - Create Category")
    class CreateCategory {

        @Test
        @DisplayName("Should create new category successfully")
        void shouldCreateCategory() throws Exception {
            String requestBody = """
                {
                    "name": "Smartphones",
                    "slug": "smartphones",
                    "description": "Mobile phones and accessories",
                    "displayOrder": 2,
                    "isActive": true,
                    "featured": false
                }
                """;

            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Smartphones"))
                    .andExpect(jsonPath("$.data.slug").value("smartphones"));
        }

        @Test
        @DisplayName("Should fail to create category with duplicate slug")
        void shouldFailWithDuplicateSlug() throws Exception {
            String requestBody = """
                {
                    "name": "Electronics Category",
                    "slug": "electronics",
                    "description": "Duplicate slug"
                }
                """;

            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail to create category with empty name")
        void shouldFailWithEmptyName() throws Exception {
            String requestBody = """
                {
                    "name": "",
                    "slug": "empty-name"
                }
                """;

            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/categories/{id} - Update Category")
    class UpdateCategory {

        @Test
        @DisplayName("Should update category successfully")
        void shouldUpdateCategory() throws Exception {
            String requestBody = """
                {
                    "name": "Updated Electronics",
                    "description": "Updated description",
                    "displayOrder": 5
                }
                """;

            mockMvc.perform(put("/api/v1/categories/{id}", testCategory.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Updated Electronics"));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent category")
        void shouldReturn404WhenUpdating() throws Exception {
            String requestBody = """
                {
                    "name": "Non-existent"
                }
                """;

            mockMvc.perform(put("/api/v1/categories/{id}", 99999)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/categories/{id} - Delete Category")
    class DeleteCategory {

        @Test
        @DisplayName("Should delete category successfully")
        void shouldDeleteCategory() throws Exception {
            mockMvc.perform(delete("/api/v1/categories/{id}", testCategory.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get("/api/v1/categories/{id}", testCategory.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent category")
        void shouldReturn404WhenDeleting() throws Exception {
            mockMvc.perform(delete("/api/v1/categories/{id}", 99999))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/categories/{id}/status - Toggle Status")
    class ToggleStatus {

        @Test
        @DisplayName("Should toggle category status")
        void shouldToggleStatus() throws Exception {
            mockMvc.perform(patch("/api/v1/categories/{id}/status", testCategory.getId())
                            .param("active", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.isActive").value(false));
        }
    }
}

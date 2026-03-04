package com.smart_ecomernce_api.smart_ecomernce_api.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PageInput {
    private int page = 0;
    private int size = 20;
    private String sortBy = "id";
    private SortDirection direction = SortDirection.ASC;

}

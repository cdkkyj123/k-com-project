package com.example.kcomproject.domain.menu.controller;

import com.example.kcomproject.domain.menu.service.MenuService;
import com.example.kcomproject.global.dto.PageResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuController.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuService menuService;

    @Test
    void getMenus_ShouldReturnOk() throws Exception {
        given(menuService.getMenus(any(), any(), any(), any(), anyInt()))
                .willReturn(PageResponseDto.ofCursor(null, 10, false, null));

        mockMvc.perform(get("/api/v1/menus"))
                .andExpect(status().isOk());
    }
}

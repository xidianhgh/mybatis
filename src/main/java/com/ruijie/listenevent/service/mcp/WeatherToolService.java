package com.ruijie.listenevent.service.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Tool：天气查询工具
 * 通过 MCP Server 以 Streamable-HTTP 模式对外暴露
 */
@Service
public class WeatherToolService {

    @Tool(description = "查询指定城市的天气信息，返回该城市的天气描述")
    public String getWeather(@ToolParam(description = "城市名称，例如：北京、上海") String city) {
        return "It's always rainy in " + city + "!";
    }
}

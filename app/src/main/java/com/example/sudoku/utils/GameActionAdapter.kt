package com.example.sudoku.utils

import com.example.sudoku.model.GameAction
import com.google.gson.*
import java.lang.reflect.Type

/**
 *  这是一个自定义的 TypeAdapter，专门用来教 Gson 如何序列化和反序列化我们的 GameAction 密封类。
 */
class GameActionAdapter : JsonSerializer<GameAction>, JsonDeserializer<GameAction> {

    // 定义一个常量作为类型字段的键名
    private companion object {
        const val TYPE_FIELD = "action_type"
    }

    // 如何将一个 GameAction 对象转换为 JSON
    override fun serialize(src: GameAction, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        // 1. 先用默认的方式将对象转换为 JSON 树
        val jsonObject = context.serialize(src).asJsonObject

        // 2. 根据对象的具体类型，添加一个 "action_type" 字段
        val typeName = when (src) {
            is GameAction.FillCell -> "FillCell"
            is GameAction.SelectCell -> "SelectCell"
            is GameAction.StartGame -> "StartGame"
        }
        jsonObject.addProperty(TYPE_FIELD, typeName)

        return jsonObject
    }

    // 如何将一个 JSON 转换为 GameAction 对象
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): GameAction {
        val jsonObject = json.asJsonObject
        // 1. 从 JSON 中读取 "action_type" 字段
        val typeName = jsonObject.get(TYPE_FIELD)?.asString

        // 2. 根据类型名称，告诉 Gson 要反序列化成哪个具体的子类
        val actualType = when (typeName) {
            "FillCell" -> GameAction.FillCell::class.java
            "SelectCell" -> GameAction.SelectCell::class.java
            "StartGame" -> GameAction.StartGame::class.java
            else -> throw JsonParseException("Unknown action type: $typeName")
        }

        return context.deserialize(jsonObject, actualType)
    }
}
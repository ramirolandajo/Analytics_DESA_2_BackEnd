package ar.edu.uade.analytics.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.gson.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AiGeminiService {

    private final JdbcTemplate jdbcTemplate;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                    (src, typeOfSrc, context) ->
                            new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>)
                    (src, typeOfSrc, context) ->
                            new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>)
                    (src, typeOfSrc, context) ->
                            new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("HH:mm:ss"))))
            .create();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String SCHEMA = """
            Base de datos: analytics_prod
            Descripción: E-commerce con usuarios, productos, compras, reseñas y stock.
            
            Reglas para generación de SQL:
            - Los alias deben ser coherentes (usar el mismo nombre corto en SELECT y JOIN).
            - product NO tiene category_id directo. La relación es product → product_category → category.
            - Si se necesita la categoría de un producto, usar JOIN product_category + JOIN category.
            - No usar p, b, c si no se definieron. Usar alias consistentes como:
              prd = product, brnd = brand, cat = category, pur = purchases, cart = carts, pci = cart_items, u = users.
            - Siempre agrupar por las columnas SELECT no agregadas.
            
                        Reglas de salida (OBLIGATORIAS):
                        - Tu respuesta debe ser EXACTAMENTE UNA consulta SQL de MySQL 8 y nada más.
                        - Debe comenzar con ```sql (tres backticks + sql) y terminar con ``` (tres backticks).
                        - No incluyas texto fuera del bloque de código, ni comentarios, ni explicaciones.
                        - La consulta debe ser un único SELECT terminado en ;\s
                        - Usa SOLO los nombres de tablas/columnas definidos abajo (sin plurales inventados).
                        - Alias consistentes: prd=product, brnd=brand, cat=category, pc=product_category, u=users, pur=purchases, cart=carts, pci=cart_items, rv=review, vw=view, scl=stock_change_log, fps=favourite_products.
                        - Para categoría: product → product_category → category (product NO tiene category_id).
                        - Para ventas: purchases → carts → cart_items → product.
                        - Agrupá todas las columnas no agregadas (ONLY_FULL_GROUP_BY activo).
            
            
            Instrucciones:
            - Todas las tablas existen con los nombres exactos que aparecen a continuación (NO usar plural).
            - Siempre usar los nombres exactos de columnas y tablas.
            - No inventar columnas ni tablas adicionales.
            - Usar JOINs explícitos en lugar de subconsultas.
            - Cuando se requiera unir con categorías, hacerlo vía product_category.
            - Cuando se necesite ventas o totales, unir en este orden:
              purchases → carts → cart_items → product
            - Para reseñas: review.product_id = product.id
            - Para stock: stock_change_log.product_id = product.id
            - Para vistas: view.product_id = product.id
            
            Tablas principales y sus columnas:
            ------------------------------------------------------------
            brand (
                id INT PK AUTO_INCREMENT,
                active BIT(1),
                brand_code INT UNIQUE,
                name VARCHAR(255)
            )
            
            category (
                id INT PK AUTO_INCREMENT,
                active BIT(1),
                category_code INT UNIQUE,
                name VARCHAR(255)
            )
            
            product (
                id INT PK AUTO_INCREMENT,
                active BIT(1),
                calification FLOAT,
                description VARCHAR(255),
                discount FLOAT,
                hero BIT(1),
                is_bestseller BIT(1),
                is_featured BIT(1),
                is_new BIT(1),
                price FLOAT,
                price_unit FLOAT,
                product_code INT,
                stock INT,
                title VARCHAR(255),
                brand_id INT FK → brand.id
            )
            
            product_category (
                product_id INT FK → product.id,
                category_id INT FK → category.id
            )
            
            product_media_src (
                product_id INT FK → product.id,
                media_src VARCHAR(255)
            )
            
            users (
                id INT PK AUTO_INCREMENT,
                account_active BIT(1),
                email VARCHAR(255) UNIQUE,
                lastname VARCHAR(255),
                name VARCHAR(255),
                password VARCHAR(255),
                role VARCHAR(255),
                session_active BIT(1)
            )
            
            carts (
                id INT PK AUTO_INCREMENT,
                external_cart_id INT,
                final_price FLOAT,
                user_id INT FK → users.id
            )
            
            cart_items (
                id INT PK AUTO_INCREMENT,
                quantity INT,
                cart_id INT FK → carts.id,
                product_id INT FK → product.id
            )
            
            purchases (
                id INT PK AUTO_INCREMENT,
                date DATETIME(6),
                direction VARCHAR(255),
                reservation_time DATETIME(6),
                status ENUM('CANCELLED', 'CONFIRMED', 'PENDING'),
                cart_id INT FK → carts.id,
                user_id INT FK → users.id
            )
            
            review (
                id BIGINT PK AUTO_INCREMENT,
                calification FLOAT,
                description VARCHAR(1000),
                product_id INT FK → product.id
            )
            
            view (
                id BIGINT PK AUTO_INCREMENT,
                product_code INT,
                viewed_at DATETIME(6),
                product_id INT FK → product.id
            )
            
            stock_change_log (
                id BIGINT PK AUTO_INCREMENT,
                product_id INT FK → product.id,
                new_stock INT,
                old_stock INT,
                quantity_changed INT,
                reason VARCHAR(255),
                changed_at DATETIME(6)
            )
            
            favourite_products (
                id BIGINT PK AUTO_INCREMENT,
                product_code INT,
                product_id INT FK → product.id,
                user_id INT FK → users.id
            )
            ------------------------------------------------------------
            
            Reglas para generación de SQL:
            - Usar alias consistentes:
              prd = product, brnd = brand, cat = category, pc = product_category,
              u = users, pur = purchases, cart = carts, pci = cart_items,
              rv = review, vw = view, scl = stock_change_log, fps = favourite_products.
            - Usar JOINs explícitos, no subconsultas.
            - Para unir productos con categorías: product → product_category → category.
            - Para unir ventas: purchases → carts → cart_items → product.
            - Para stock: usar stock_change_log (old_stock, new_stock, quantity_changed, changed_at).
            - Siempre agrupar las columnas no agregadas (modo only_full_group_by activo).
            - Generar consultas válidas solo con SELECT terminado en punto y coma.
            
            Notas semánticas:
            - "nombre del producto" → product.title
            - "marca" → brand.name
            - "categoría" → category.name
            - "usuario" → users.name + users.lastname
            - "ventas" → purchases + carts + cart_items + product
            - "reseñas" → review.calification, review.description
            - "vistas" → view.viewed_at
            - "stock" → stock_change_log.new_stock y quantity_changed
            ------------------------------------------------------------
            
            Reglas adicionales para calidad de consulta y nivel de detalle:
            - Siempre incluir columnas numéricas o de fecha cuando existan (por ejemplo: quantity, price, new_stock, date, calification, final_price).
            - Nunca limitar la consulta solo a nombres o títulos de productos.
            - Si la consulta pide “bajo stock”, incluir también la cantidad exacta (new_stock).
            - Si pide ventas, incluir cantidad vendida, total monetario y fechas.
            - Si se pide información de usuarios, incluir nombre, email y cualquier métrica asociada (cantidad de compras, vistas, etc.).
            - Cuando se aplique un filtro o agregación, incluir todas las columnas relevantes para análisis ejecutivo.
            - Priorizar resultados que aporten contexto numérico o temporal para la toma de decisiones.
            
            """;

    public AiGeminiService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String answer(String userQuery) {
        try {
            // 🔑 Establecer la API Key como variable de entorno reconocida
            System.setProperty("GOOGLE_API_KEY", geminiApiKey);

            // 🧠 Inicializar el cliente de Gemini
            Client client = new Client();

            // 🧩 Generar SQL a partir de la pregunta del usuario
            String prompt = SCHEMA + """

IMPORTANTE:
- Debes devolver EXCLUSIVAMENTE una consulta SQL de MySQL 8.
- La respuesta DEBE comenzar con ```sql y terminar con ```.
- No incluyas texto, explicaciones ni comentarios.
- Si no puedes generar una consulta válida, devolvé literalmente:
SELECT 'No se pudo generar una consulta válida' AS error;
""".stripIndent() + "\nUsuario pregunta: " + userQuery;

            GenerateContentResponse sqlResponse = client.models.generateContent(
                    "gemini-2.0-flash",
                    prompt,
                    null
            );

            // 🧹 Limpiar el bloque SQL (eliminar ```sql ... ```)
            String sqlRaw = sqlResponse.text().trim();
            String sql = sqlRaw
                    .replaceAll("(?s)```sql", "")
                    .replaceAll("(?s)```", "")
                    .trim();

            System.out.println("🧠 [AI SQL] => " + sql);

            if (!sql.toLowerCase().startsWith("select")) {
                return "⚠️ Solo se permiten consultas SELECT.";
            }

            // 🧮 Ejecutar la consulta en MySQL
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            String jsonResult = gson.toJson(result);

            // 💬 Explicar el resultado sin Markdown ni saltos de línea
            String explainPrompt = """
Estos son los resultados del SQL ejecutado:
%s

Actuá como un analista de datos senior especializado en e-commerce.
Describí con precisión qué muestran los datos, incluyendo:
- nombres de productos, categorías, marcas o usuarios
- fechas, cantidades, precios o métricas relevantes
- patrones, picos o valores destacados
- relaciones entre variables (por ejemplo, producto y ventas o stock)
- y cualquier hallazgo que pueda impactar decisiones ejecutivas.

Además, analizá el formato de los datos y sugerí el tipo de gráfico más adecuado para visualizarlos (por ejemplo: 'bar', 'line', 'pie', 'area').
Devolvé la respuesta en formato JSON con las siguientes claves:
{
  "summary": "texto detallado con la descripción analítica en una sola línea, sin formato Markdown, sin saltos de línea ni símbolos especiales",
  "chartType": "bar | line | pie | area (según corresponda al tipo de dato)",
  "data": [ { ... } ] // usá los datos originales del SQL sin alterar nombres de columnas
}

Reglas estrictas de salida:
- No uses Markdown ni símbolos especiales
- No inventes columnas ni valores
- Si hay pocos registros, mencioná cada uno con su valor numérico
- Si hay muchos registros, hacé un resumen ejecutivo con totales o tendencias clave
- Todo debe estar en una sola línea JSON sin texto adicional fuera de las llaves.
""".formatted(jsonResult);



            GenerateContentResponse explainResponse = client.models.generateContent("gemini-2.0-flash", explainPrompt, null);

            String rawJson = explainResponse.text()
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            System.out.println("🧠 [AI RAW JSON] => " + rawJson);

            Map<String, Object> aiResponse = gson.fromJson(rawJson, Map.class);
            String summary = (String) aiResponse.getOrDefault("summary", "");
            String chartType = (String) aiResponse.getOrDefault("chartType", "bar");
            List<Map<String, Object>> data = (List<Map<String, Object>>) aiResponse.getOrDefault("data", result);

            // Limpiar texto
            String explanation = summary
                    .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                    .replace("\\n", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            // 🧩 Normalizar claves y preparar chartData universal
            List<Map<String, Object>> normalizedData = new ArrayList<>();

            for (Map<String, Object> row : result) {
                Map<String, Object> newRow = new HashMap<>(row);

                // ====== 1️⃣ Normalización de texto (eje X o label) ======
                String textKey = null;
                if (row.containsKey("nombre_producto")) textKey = "nombre_producto";
                else if (row.containsKey("product_title")) textKey = "product_title";
                else if (row.containsKey("title")) textKey = "title";
                else if (row.containsKey("product_name")) textKey = "product_name";
                else if (row.containsKey("categoria")) textKey = "categoria";
                else if (row.containsKey("category_name")) textKey = "category_name";
                else if (row.containsKey("brand_name")) textKey = "brand_name";
                else if (row.containsKey("user_name")) textKey = "user_name";
                else if (row.containsKey("name")) textKey = "name";

                if (textKey != null) {
                    newRow.put("nombre_producto", row.get(textKey));
                } else if (row.containsKey("product_id")) {
                    newRow.put("nombre_producto", "Producto " + row.get("product_id"));
                } else {
                    newRow.put("nombre_producto", "Sin nombre");
                }

                // ====== 2️⃣ Normalización de valor numérico (eje Y o dataset principal) ======
                String numericKey = null;
                if (row.containsKey("total_revenue")) numericKey = "total_revenue";
                else if (row.containsKey("ingresos_totales")) numericKey = "ingresos_totales";
                else if (row.containsKey("total_quantity_sold")) numericKey = "total_quantity_sold";
                else if (row.containsKey("cantidad_unidades_vendidas")) numericKey = "cantidad_unidades_vendidas";
                else if (row.containsKey("new_stock")) numericKey = "new_stock";
                else if (row.containsKey("stock")) numericKey = "stock";
                else if (row.containsKey("calification")) numericKey = "calification";
                else if (row.containsKey("final_price")) numericKey = "final_price";
                else if (row.containsKey("quantity_changed")) numericKey = "quantity_changed";
                else if (row.containsKey("view_count")) numericKey = "view_count";
                else if (row.containsKey("average_calification")) numericKey = "average_calification";



                if (numericKey != null) {
                    Object value = row.get(numericKey);
                    if (value instanceof Number) {
                        newRow.put("valor_numerico", ((Number) value).doubleValue());
                    } else {
                        try {
                            newRow.put("valor_numerico", Double.parseDouble(value.toString()));
                        } catch (Exception ignored) {
                            newRow.put("valor_numerico", 0.0);
                        }
                    }
                } else {
                    newRow.put("valor_numerico", 0.0);
                }

                // ====== 3️⃣ Normalización temporal (fechas para line chart) ======
                String dateKey = null;
                if (row.containsKey("changed_at")) dateKey = "changed_at";
                else if (row.containsKey("date")) dateKey = "date";
                else if (row.containsKey("viewed_at")) dateKey = "viewed_at";
                else if (row.containsKey("reservation_time")) dateKey = "reservation_time";


                if (dateKey != null) {
                    newRow.put("fecha_evento", String.valueOf(row.get(dateKey)));
                }

                normalizedData.add(newRow);
            }

// ====== 4️⃣ Construcción de chartData ======
            List<String> labels = normalizedData.stream()
                    .map(r -> String.valueOf(r.get("nombre_producto")))
                    .toList();

            List<Double> values = normalizedData.stream()
                    .map(r -> ((Number) r.getOrDefault("valor_numerico", 0.0)).doubleValue())
                    .toList();

// ====== 5️⃣ Detección del tipo de gráfico ======
            String chartTypeDetected = "bar";
            if (!normalizedData.isEmpty() && normalizedData.get(0).containsKey("fecha_evento")) {
                chartTypeDetected = "line";
            } else if (labels.size() <= 5 && values.stream().mapToDouble(Double::doubleValue).sum() > 0) {
                chartTypeDetected = "pie";
            }


            // === 🔢 FORMATEO DE VALORES NUMÉRICOS ===
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

// Redondear y agregar formato de moneda
            for (Map<String, Object> row : normalizedData) {
                Object val = row.get("valor_numerico");
                if (val instanceof Number) {
                    double rounded = Math.round(((Number) val).doubleValue() * 100.0) / 100.0;
                    row.put("valor_numerico", rounded);
                    row.put("valor_formateado", currencyFormat.format(rounded));
                }
            }

// Redondear datasets también
            List<Double> roundedValues = values.stream()
                    .map(v -> Math.round(v * 100.0) / 100.0)
                    .toList();

// Detectar tipo de valor (para etiqueta del dataset)
            String labelName = "Valores";
            if (!normalizedData.isEmpty()) {
                Map<String, Object> first = normalizedData.get(0);
                if (first.containsKey("total_revenue") || first.containsKey("ingresos_totales"))
                    labelName = "Ingresos Totales ($)";
                else if (first.containsKey("total_quantity_sold") || first.containsKey("cantidad_unidades_vendidas"))
                    labelName = "Unidades Vendidas";
                else if (first.containsKey("new_stock") || first.containsKey("stock"))
                    labelName = "Stock Disponible";
                else if (first.containsKey("calification"))
                    labelName = "Calificación Promedio";

            }

// Reconstruir chartData final con redondeo y label dinámico
            Map<String, Object> chartData = Map.of(
                    "labels", labels,
                    "datasets", List.of(Map.of(
                            "label", labelName,
                            "data", roundedValues
                    ))
            );


// ====== 7️⃣ Retorno final (JSON completo y consistente) ======
            return gson.toJson(Map.of(
                    "response", explanation,
                    "chartType", chartTypeDetected,
                    "data", normalizedData,
                    "chartData", chartData
            ));


        } catch (BadSqlGrammarException e) {
            String msg = e.getSQLException() != null ? e.getSQLException().getMessage() : e.getMessage();
            return "⚠️ SQL inválido: " + msg;
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error procesando la consulta: " + e.getMessage();
        }
    }
}
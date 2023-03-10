public class Application {

	private static ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) {
		var dbResult = multiColumnGetAll("Dogs", Map.of(
				"name", String.class,
				"params", Map.class,
				"age", Integer.class));
		System.out.println(dbResult);
		var dogs = dbResult.stream()
				.map(map -> new Dog(map.get("name"), map.get("params"), map.get("age")))
				.collect(Collectors.toList());
		System.out.println(dogs);
	}

	private static List<AutoCastMap<String, Object>> multiColumnGetAll(String table, Map<String, Class<?>> cols) {
		List<AutoCastMap<String, Object>> result = new ArrayList<>();
		String select = String.join(", ", cols.keySet());
		try(Connection conn = newConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT "+select+" FROM "+table)) {
			System.out.println(stmt);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AutoCastMap<String, Object> row = new AutoCastMap<>();
				for (var e : cols.entrySet()) {
					var col = e.getKey();
					var valType = e.getValue();
					var valStr = rs.getString(col);
					var val = valType == String.class ? valStr : mapper.readValue(valStr, valType);
					row.put(col, val);
				}
				result.add(row);
			}
		} catch (SQLException | JsonProcessingException e) {
			e.printStackTrace();
		}

		return result;
	}

	private static Connection newConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres?user=postgres&password=password");
	}

	private static class AutoCastMap<K, V> extends HashMap<K, V> {
		public <T> T get(String key) {
			return (T) super.get(key);
		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	private static class Dog {
		private String name;
		private Map<String, Object> params;
		private int age;
	}
	
	 private static void s(String id, String table, Map<String, Object> cols) {
        var temp = new LinkedHashMap<String, Object>();
        temp.put("id", id);
        temp.putAll(cols);
        cols = temp;
        var colList = String.join(",", cols.keySet());
        var valList = cols.keySet().stream().map(s -> "?").collect(Collectors.joining(","));
        cols.remove("id");
        var setList = cols.keySet().stream().map(o -> o + "=?").collect(Collectors.joining(", "));
        PreparedStatement stmt = new PreparedStatement(
                "INSERT INTO "+table+" ("+colList+") " +
                   "VALUES ("+valList+") " +
                   "ON CONFLICT (id) SET "+setList+" " +
                   "WHERE id = "+id);
        stmt.set(1, id);
        int i = 2;
        for (var v : cols.values()) {
            stmt.set(i, v);
            stmt.set(i+cols.size(), v);
            i++;
        }
    }

}

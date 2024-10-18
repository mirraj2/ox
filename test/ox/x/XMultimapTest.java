package ox.x;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

public class XMultimapTest {

  private XMultimap<String, String> multimap;

  @Before
  public void setUp() {
    multimap = XMultimap.create();
    multimap.put("key1", "value1");
    multimap.put("key1", "value2");
    multimap.put("key2", "value3");
  }

  @Test
  public void testGet() {
    XList<String> values = multimap.get("key1");
    assertEquals(2, values.size());
    assertTrue(values.contains("value1"));
    assertTrue(values.contains("value2"));

    XList<String> emptyValues = multimap.get("missingKey");
    assertTrue(emptyValues.isEmpty());
  }

  @Test
  public void testGetSet() {
    XSet<String> values = multimap.getSet("key1");
    assertEquals(2, values.size());
    assertTrue(values.contains("value1"));
    assertTrue(values.contains("value2"));
  }

  @Test
  public void testValuesList() {
    XList<XList<String>> valuesList = multimap.valuesList();
    assertEquals(2, valuesList.size());
    assertTrue(valuesList.get(0).contains("value1"));
    assertTrue(valuesList.get(0).contains("value2"));
    assertTrue(valuesList.get(1).contains("value3"));
  }

  @Test
  public void testKeySet() {
    XSet<String> keySet = multimap.keySet();
    assertEquals(2, keySet.size());
    assertTrue(keySet.contains("key1"));
    assertTrue(keySet.contains("key2"));
  }

  @Test
  public void testToMap() {
    XMap<String, XList<String>> map = multimap.toMap();
    assertEquals(2, map.size());
    assertTrue(map.get("key1").contains("value1"));
    assertTrue(map.get("key1").contains("value2"));
    assertTrue(map.get("key2").contains("value3"));
  }

  @Test
  public void testToMapWithReducer() {
    XMap<String, Integer> map = multimap.toMap(values -> values.size());
    assertEquals(2, map.size());
    assertEquals(Integer.valueOf(2), map.get("key1"));
    assertEquals(Integer.valueOf(1), map.get("key2"));
  }

  @Test
  public void testToListWithMappingFunction() {
    XList<String> list = multimap.toList((key, values) -> key + ":" + values.size());
    assertEquals(2, list.size());
    assertTrue(list.contains("key1:2"));
    assertTrue(list.contains("key2:1"));
  }

  @Test
  public void testInvert() {
    XMap<String, String> inverted = multimap.invert();
    assertEquals(3, inverted.size());
    assertEquals("key1", inverted.get("value1"));
    assertEquals("key1", inverted.get("value2"));
    assertEquals("key2", inverted.get("value3"));
  }

  @Test
  public void testInvertMultimap() {
    XMultimap<String, String> inverted = multimap.invertMultimap();
    assertEquals(3, inverted.size());
    assertTrue(inverted.get("value1").contains("key1"));
    assertTrue(inverted.get("value2").contains("key1"));
    assertTrue(inverted.get("value3").contains("key2"));
  }

  @Test
  public void testTransformKeys() {
    XMultimap<String, String> transformed = multimap.transformKeys(key -> key + "_transformed");
    assertTrue(transformed.keySet().contains("key1_transformed"));
    assertTrue(transformed.keySet().contains("key2_transformed"));
  }

  @Test
  public void testTransformValues() {
    XMultimap<String, String> transformed = multimap.transformValues(value -> value + "_transformed");
    assertTrue(transformed.get("key1").contains("value1_transformed"));
    assertTrue(transformed.get("key2").contains("value3_transformed"));
  }

  @Test
  public void testTransformValuesWithBiFunction() {
    XMultimap<String, String> transformed = multimap.transformValues((key, value) -> key + "_" + value);
    assertTrue(transformed.get("key1").contains("key1_value1"));
    assertTrue(transformed.get("key1").contains("key1_value2"));
    assertTrue(transformed.get("key2").contains("key2_value3"));
  }

  @Test
  public void testTransformKeysAndValues() {
    XMultimap<String, String> transformed = multimap.transform(
        key -> key + "_transformed",
        value -> value + "_transformed");
    assertTrue(transformed.get("key1_transformed").contains("value1_transformed"));
    assertTrue(transformed.get("key1_transformed").contains("value2_transformed"));
    assertTrue(transformed.get("key2_transformed").contains("value3_transformed"));
  }

  @Test
  public void testKeysAreOnlyTransformedOnce() {
    AtomicInteger keyTransformCount = new AtomicInteger(0);
    XMultimap<String, String> transformed = multimap.transform(
        key -> key + "_transformed" + keyTransformCount.incrementAndGet(),
        value -> value + "_transformed");
    assertTrue(transformed.get("key1_transformed1").contains("value1_transformed"));
    assertTrue(transformed.get("key1_transformed1").contains("value2_transformed"));
    assertTrue(transformed.get("key2_transformed2").contains("value3_transformed"));
  }

  @Test
  public void testTransformKeysAndValuesWithBiFunction() {
    XMultimap<String, String> transformed = multimap.transform(
        key -> key + "_transformed",
        (key, value) -> key + "_" + value);
    assertTrue(transformed.get("key1_transformed").contains("key1_value1"));
    assertTrue(transformed.get("key1_transformed").contains("key1_value2"));
    assertTrue(transformed.get("key2_transformed").contains("key2_value3"));
  }

  @Test
  public void testHasData() {
    assertTrue(multimap.hasData());
    multimap.clear();
    assertFalse(multimap.hasData());
  }

  @Test
  public void testFilter() {
    XMultimap<String, String> filtered = multimap.filter((key, value) -> key.equals("key1"));
    assertTrue(filtered.containsKey("key1"));
    assertFalse(filtered.containsKey("key2"));
  }

  @Test
  public void testFilterWithComplexPredicate() {
    XMultimap<String, String> filtered = multimap.filter((key, value) -> key.equals("key1") && value.contains("1"));
    assertTrue(filtered.containsKey("key1"));
    assertTrue(filtered.get("key1").contains("value1"));
    assertFalse(filtered.get("key1").contains("value2"));
  }

  @Test
  public void testAsXMap() {
    XMap<String, XList<String>> map = multimap.asXMap();
    assertEquals(2, map.size());
    assertTrue(map.get("key1").contains("value1"));
    assertTrue(map.get("key1").contains("value2"));
    assertTrue(map.get("key2").contains("value3"));
  }

}

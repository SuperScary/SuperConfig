package net.superscary.superconfig.value.wrappers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListValueTest {

    private ListValue<String> listValue;

    @BeforeEach
    void setUp() {
        listValue = new ListValue<>(Arrays.asList("test1", "test2"));
    }

    @Test
    void testGet() {
        List<String> value = listValue.get();
        assertNotNull(value);
        assertEquals(2, value.size());
        assertEquals("test1", value.get(0));
        assertEquals("test2", value.get(1));
    }

    @Test
    void testSet() {
        List<String> newValue = Arrays.asList("new1", "new2");
        listValue.set(newValue);
        
        List<String> current = listValue.get();
        assertEquals(2, current.size());
        assertEquals("new1", current.get(0));
        assertEquals("new2", current.get(1));
    }

    @Test
    void testAdd() {
        listValue.add("test3");
        List<String> value = listValue.get();
        assertEquals(3, value.size());
        assertEquals("test3", value.get(2));
    }

    @Test
    void testRemove() {
        listValue.remove("test1");
        List<String> value = listValue.get();
        assertEquals(1, value.size());
        assertEquals("test2", value.get(0));
    }

    @Test
    void testClear() {
        listValue.clear();
        List<String> value = listValue.get();
        assertTrue(value.isEmpty());
    }

    @Test
    void testAddAll() {
        listValue.addAll(Arrays.asList("test3", "test4"));
        List<String> value = listValue.get();
        assertEquals(4, value.size());
        assertEquals("test3", value.get(2));
        assertEquals("test4", value.get(3));
    }

    @Test
    void testDefaultConstructor() {
        ListValue<String> emptyList = new ListValue<>();
        assertNull(emptyList.get());
    }
} 
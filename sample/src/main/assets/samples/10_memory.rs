use std::collections::HashMap;

#[derive(Debug, Clone)]
pub struct LruCache<V> {
    store: HashMap<String, V>,
    capacity: usize,
}

impl<V> LruCache<V> {
    pub fn new(capacity: usize) -> Self {
        Self { store: HashMap::with_capacity(capacity), capacity }
    }

    pub fn get(&self, key: &str) -> Option<&V> {
        self.store.get(key)
    }

    /// Inserts a value; returns the old value if the key existed.
    pub fn insert(&mut self, key: String, value: V) -> Result<Option<V>, &'static str> {
        if self.store.len() >= self.capacity && !self.store.contains_key(&key) {
            return Err("cache full — eviction not implemented");
        }
        Ok(self.store.insert(key, value))
    }
}

fn main() {
    let mut cache: LruCache<i32> = LruCache::new(4);
    cache.insert("answer".to_owned(), 42).unwrap();
    match cache.get("answer") {
        Some(v) => println!("Found: {v}"),
        None    => println!("Cache miss"),
    }
}
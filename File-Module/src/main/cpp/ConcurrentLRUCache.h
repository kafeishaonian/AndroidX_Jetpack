//
// Created by 64860 on 2025/6/16.
//

#ifndef ANDROIDX_JETPACK_CONCURRENTLRUCACHE_H
#define ANDROIDX_JETPACK_CONCURRENTLRUCACHE_H

#include <list>
#include <unordered_map>
#include <shared_mutex>
#include <optional>

using namespace std;

template <typename Key, typename Value>
class ConcurrentLRUCache {

public:
    explicit ConcurrentLRUCache(size_t capacity) : _capacity(capacity > 0 ? capacity : 1), _size(0) {}

    bool put(const Key& key, Value&& value){
        unique_lock lock(m_mutex);
        return put_impl(key, std::forward<Value>(value));
    }

    bool put(const Key& key, const Value& value) {
        Value temp = value;
        return put(key, std::move(temp));
    }

    optional<Value> get(const Key& key) {
        shared_lock lock(m_mutex);

        auto it = _map.find(key);
        if (it == _map.end()) {
            return nullopt;
        }

        Value value = it->second->second;
        lock.unlock();

        promote_locked(key);
        return value;
    }

    bool remove(const Key& key) {
        unique_lock lock(m_mutex);
        auto it = _map.find(key);
        if (it == _map.end()) {
            return false;
        }

        _list.erase(it->second);
        _map.erase(it);
        _size--;
        return true;
    }

    bool contains(const Key& key) const {
        shared_lock lock(m_mutex);
        return _map.find(key) != _map.end();
    }

    size_t size() const {
        shared_lock lock(m_mutex);
        return _size;
    }

    void clear() {
        unique_lock lock(m_mutex);
        _list.clear();
        _map.clear();
        _size = 0;
    }

    size_t capacity() const {
        return _capacity;
    }

private:
    using ListType = list<pair<Key, Value>>;
    using ListIterator = typename ListType::iterator;

    bool put_impl(const Key& key, Value&& value) {
        auto it = _map.find(key);
        if (it != _map.end()) {
            it->second->second = std::move(value);
            _list.splice(_list.begin(), _list, it->second);
            return true;
        }

        if (_size >= _capacity) {
            auto last = --_list.end();
            _map.erase(last->first);
            _list.pop_back();
            _size--;
        }

        _list.emplace_front(key, std::forward<Value>(value));
        _map[key] = _list.begin();
        _size++;
        return true;
    }

    void promote_locked(const Key& key) {
        unique_lock lock(m_mutex, defer_lock);
        if (!lock.try_lock()) {
            return;
        }

        auto it = _map.find(key);
        if (it != _map.end()) {
            _list.splice(_list.begin(), _list, it->second);
        }
    }

    ListType _list;
    unordered_map<Key, ListIterator> _map;
    mutable shared_mutex m_mutex;
    size_t _capacity;
    size_t _size;

};


#endif //ANDROIDX_JETPACK_CONCURRENTLRUCACHE_H

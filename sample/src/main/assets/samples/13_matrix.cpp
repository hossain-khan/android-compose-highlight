#include <algorithm>
#include <iostream>
#include <memory>
#include <stdexcept>
#include <vector>

template <typename T>
class Stack {
public:
    void push(T value) { data_.push_back(std::move(value)); }

    T pop() {
        if (data_.empty()) throw std::underflow_error("Stack is empty");
        T top = std::move(data_.back());
        data_.pop_back();
        return top;
    }

    [[nodiscard]] bool empty() const noexcept { return data_.empty(); }
    [[nodiscard]] std::size_t size() const noexcept { return data_.size(); }

private:
    std::vector<T> data_;
};

int main() {
    auto s = std::make_unique<Stack<int>>();
    for (int i : {3, 1, 4, 1, 5, 9, 2, 6}) s->push(i);

    std::cout << "Size: " << s->size() << "\nPopping: ";
    while (!s->empty()) std::cout << s->pop() << ' ';
    std::cout << '\n';

    // Lambda + algorithm example
    std::vector<int> nums = {5, 3, 8, 1, 9, 2};
    std::sort(nums.begin(), nums.end(), [](int a, int b) { return a > b; });
    for (auto n : nums) std::cout << n << ' ';
    return 0;
}
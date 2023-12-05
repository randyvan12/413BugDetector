#include <stdio.h>

int main() {
    int actualValue = 10;  // An actual integer value
    int *ptr = &actualValue;  // Pointer initialized to the address of actualValue
    int value;

    // Dereferencing a pointer that is not NULL
    value = *ptr;

    printf("Value: %d\n", value);

    return 0;
}
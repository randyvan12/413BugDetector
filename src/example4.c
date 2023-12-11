#include <stdio.h>

int main() {
    int *ptr = NULL;  // Pointer initialized to NULL
    int actualValue = 42;
    int condition = 1;  // This variable controls the if-else condition

    // Conditional assignment to ptr
    if (condition) {
        ptr = &actualValue;  // Assigning ptr to the address of actualValue
    } else {
        ptr = NULL;  // Keeping ptr as NULL
    }

    // Only dereference the pointer if the condition is true
    int value;
    if (condition) {
        value = *ptr;
    } else {
        value = -1;  // Default value when condition is false
    }

    printf("Value: %d\n", value);

    return 0;
}
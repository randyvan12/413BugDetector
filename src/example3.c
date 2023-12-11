#include <stdio.h>

int main() {
    int *ptr = NULL;  // Pointer initialized to NULL
    int actualValue = 42;
    int condition = 0;  // This variable controls the if-else condition

    // Conditional assignment to ptr
    if (condition) {
        ptr = &actualValue;  // Assigning ptr to the address of actualValue
    } else {
        ptr = NULL;  // Keeping ptr as NULL
    }

    // Dereferencing the pointer regardless of the condition
    int value = *ptr;

    printf("Value: %d\n", value);

    return 0;
}
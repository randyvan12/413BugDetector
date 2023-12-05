#include <stdio.h>

int main() {
    int *ptr = NULL;  // Pointer initialized to NULL
    int value;

    // Attempting to dereference a NULL pointer
    value = *ptr;

    printf("Value: %d\n", value);

    return 0;
}
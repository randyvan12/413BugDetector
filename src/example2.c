#include <stdio.h>
#include <stdlib.h>

// Function to fill an array with values
void fillArray(int *arr, int size) {
    for (int i = 0; i < size; i++) {
        arr[i] = i * 10;
    }
}

int main() {
    int *array = NULL;
    int size = 10, sum = 0;

    // Conditionally allocate memory (but in this case, we won't)
    if (size > 100) {
        array = (int *)malloc(size * sizeof(int));
    }

    // Fill the array with values
    fillArray(array, size);

    // Calculate the sum of array elements
    for (int i = 0; i < size; i++) {
        sum += array[i];
    }

    printf("Sum: %d\n", sum);

    // Free memory if allocated
    if (array != NULL) {
        free(array);
    }

    return 0;
}
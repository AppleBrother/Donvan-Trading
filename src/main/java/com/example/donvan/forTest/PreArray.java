package com.example.donvan.forTest;

import java.util.HashMap;
import java.util.Map;

public class PreArray {
    public static void main(String[] args) {
        PreArray preArray = new PreArray();
        int[] nums = {1, 2, 3};
        int k = 3;
        int result = preArray.subarraySum(nums, k);
        int result01 = preArray.subarraySum01(nums, k);
        System.out.println("Number of subarrays with sum " + k + ": " + result01);
    }

    public int subarraySum(int[] nums, int k) {
        Map<Integer, Integer> preSumMap = new HashMap<>();
        //preSumMap.put(0, 1);
        int currentSum = 0;
        int count = 0;
        for (int num : nums) {
            currentSum += num;
            int target = currentSum - k;
            if (preSumMap.containsKey(target)) {
                count += preSumMap.get(target);
            }
            preSumMap.put(currentSum, preSumMap.getOrDefault(currentSum, 0) + 1);
        }
        return count;
    }

    public int subarraySum01(int[] nums, int k) {
        int count = 0;

        for (int i = 0; i < nums.length; i++) {
            int currentSum = 0;
            for (int j = i; j < nums.length; j++) {
                currentSum += nums[j];
                if (currentSum == k) {
                    count++;
                }
            }
        }
        return count;
    }
}

package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.ComponentInstance;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ComponentDigest {

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String digest(ComponentInstance instance) {
        MessageDigest digest = getDigestAlgorithm();
        iterateAndDigestInto(instance, digest);
        byte[] digestedBytes = digest.digest();
        return bytesToHex(digestedBytes);
    }

    private static MessageDigest getDigestAlgorithm() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("We need SHA-256 in order calculate the component hashcode.", e);
        }
        return digest;
    }

    private static void iterateAndDigestInto(ComponentInstance instance, MessageDigest digest) {
        Set<Integer> guard = new HashSet<>();
        Deque<ComponentInstance> queue = new LinkedList<>();
        queue.add(instance);
        while(!queue.isEmpty()) {
            ComponentInstance cursor = queue.pop();
            if(cursor instanceof CIIndexed) {
                CIIndexed ciIndexed = (CIIndexed) cursor;
                if(guard.contains(ciIndexed.getIndex())) {
                    continue;
                } else {
                    guard.add(ciIndexed.getIndex());
                }
            }
            digestInto(instance, digest);
            instance.getSatisfactionOfRequiredInterfaces()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() != null)
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .forEachOrdered(entry -> queue.add(entry.getValue()));
        }
    }

    private static void digestInto(ComponentInstance instance, MessageDigest digest) {
        // Add what component is used
        digest.update(instance.getComponent().getName().getBytes());
        instance.getParameterValues()
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEachOrdered(entry -> {
                    digest.update(entry.getKey().getBytes());
                    digest.update(entry.getValue().getBytes());
                });
    }

}

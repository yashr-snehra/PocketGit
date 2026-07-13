package pocketgit;

import java.util.ArrayList;
import java.util.List;

public final class DiffUtil {

    private DiffUtil() { }

    public static List<String> diff(List<String> a, List<String> b) {
        int n = a.size(), m = b.size();
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--)
            for (int j = m - 1; j >= 0; j--)
                lcs[i][j] = a.get(i).equals(b.get(j))
                        ? lcs[i + 1][j + 1] + 1
                        : Math.max(lcs[i + 1][j], lcs[i][j + 1]);

        List<String> out = new ArrayList<>();
        int i = 0, j = 0;
        while (i < n && j < m) {
            if (a.get(i).equals(b.get(j))) {
                out.add("  " + a.get(i));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                out.add("- " + a.get(i));
                i++;
            } else {
                out.add("+ " + b.get(j));
                j++;
            }
        }
        while (i < n) out.add("- " + a.get(i++));
        while (j < m) out.add("+ " + b.get(j++));
        return out;
    }
}

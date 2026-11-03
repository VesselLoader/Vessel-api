// ==============================================================================
// VESSEL-API — VesselComponentSnapshot.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselComponentSnapshot contract
//          bridging Adventure/Vanilla text components across VUSA.
// ==============================================================================

package dev.vessel.api.text;

import java.util.Objects;

/**
 * Immutable snapshot of a rich text component bridged across Vanilla, Adventure,
 * and cross-loader chat/UI subsystems within the VESSEL Universal Super-API (VUSA).
 *
 * @version 1.0.0
 */
public final class VesselComponentSnapshot {

    private final String plainText;
    private final String hexColor;
    private final int childCount;

    /**
     * Constructs a new {@link VesselComponentSnapshot}.
     *
     * @param plainText  the flattened or root plain text content (must not be {@code null}, may be empty)
     * @param hexColor   the CSS-style hex color string such as {@code #FFFFFF} (must not be {@code null} or blank)
     * @param childCount the number of child sibling components (must be {@code >= 0})
     * @throws NullPointerException     if {@code plainText} or {@code hexColor} is {@code null}
     * @throws IllegalArgumentException if {@code hexColor} is blank or {@code childCount < 0}
     */
    public VesselComponentSnapshot(String plainText, String hexColor, int childCount) {
        Objects.requireNonNull(plainText, "plainText must not be null");
        Objects.requireNonNull(hexColor, "hexColor must not be null");
        if (hexColor.isBlank()) {
            throw new IllegalArgumentException("hexColor must not be blank");
        }
        if (childCount < 0) {
            throw new IllegalArgumentException("childCount must not be negative");
        }
        this.plainText = plainText;
        this.hexColor = hexColor;
        this.childCount = childCount;
    }

    /**
     * Returns the plain text content of this component snapshot.
     *
     * @return plain text content
     */
    public String getPlainText() {
        return this.plainText;
    }

    /**
     * Returns the hex color code associated with this component snapshot.
     *
     * @return hex color code
     */
    public String getHexColor() {
        return this.hexColor;
    }

    /**
     * Returns the number of child components attached to this component.
     *
     * @return non-negative child component count
     */
    public int getChildCount() {
        return this.childCount;
    }

    /**
     * Checks whether this component has neither plain text content nor child components.
     *
     * @return {@code true} if {@code plainText.isEmpty() && childCount == 0}
     */
    public boolean isEmpty() {
        return this.plainText.isEmpty() && this.childCount == 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VesselComponentSnapshot other)) {
            return false;
        }
        return this.childCount == other.childCount
                && this.plainText.equals(other.plainText)
                && this.hexColor.equals(other.hexColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.plainText, this.hexColor, this.childCount);
    }

    @Override
    public String toString() {
        return "VesselComponentSnapshot{"
                + "plainText='" + this.plainText + '\''
                + ", hexColor='" + this.hexColor + '\''
                + ", childCount=" + this.childCount
                + '}';
    }
}

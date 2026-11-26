package com.staybook.staybook.setting;

import jakarta.persistence.*;

@Entity
@Table(name = "settings", uniqueConstraints = @UniqueConstraint(name = "uk_setting_key", columnNames = "setting_key"))
public class Setting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "setting_key", nullable = false, length = 100) private String key;
    @Column(name = "value", nullable = false, length = 500) private String value;
    @Column(name = "setting_value", nullable = false, length = 500) private String legacyCompatibleValue;
    protected Setting() {}
    public Setting(String key, String value) {
        this.key = key; this.value = value; this.legacyCompatibleValue = value;
    }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public void setValue(String value) {
        this.value = value;
        this.legacyCompatibleValue = value;
    }
}

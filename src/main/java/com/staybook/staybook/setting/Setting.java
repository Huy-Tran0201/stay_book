package com.staybook.staybook.setting;

import jakarta.persistence.*;

@Entity
@Table(name = "settings", uniqueConstraints = @UniqueConstraint(name = "uk_setting_key", columnNames = "setting_key"))
public class Setting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "setting_key", nullable = false, length = 100) private String key;
    @Column(nullable = false, length = 500) private String value;
    protected Setting() {}
    public Setting(String key, String value) { this.key = key; this.value = value; }
}

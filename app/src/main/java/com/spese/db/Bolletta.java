package com.spese.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bollette")
public class Bolletta {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String tipo;
    private double importo;
    private int mese;
    private int anno;
    private long creatoIl;

    public Bolletta(String tipo, double importo, int mese, int anno) {
        this.tipo = tipo;
        this.importo = importo;
        this.mese = mese;
        this.anno = anno;
        this.creatoIl = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getImporto() { return importo; }
    public void setImporto(double importo) { this.importo = importo; }

    public int getMese() { return mese; }
    public void setMese(int mese) { this.mese = mese; }

    public int getAnno() { return anno; }
    public void setAnno(int anno) { this.anno = anno; }

    public long getCreatoIl() { return creatoIl; }
    public void setCreatoIl(long creatoIl) { this.creatoIl = creatoIl; }
}

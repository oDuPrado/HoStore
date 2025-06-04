package model;

public class ColecaoModel {
    private String id;
    private String name;
    private String series;
    private String printedTotal;
    private String total;
    private String releaseDate;

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSeries() {
        return series;
    }

    public String getPrintedTotal() {
        return printedTotal;
    }

    public String getTotal() {
        return total;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public void setPrintedTotal(String printedTotal) {
        this.printedTotal = printedTotal;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    @Override
    public String toString() {
        return name;
    }

}

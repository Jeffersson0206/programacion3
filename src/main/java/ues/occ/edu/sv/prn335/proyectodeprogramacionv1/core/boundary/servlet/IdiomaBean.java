package ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.boundary.servlet;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;


import java.io.Serializable;
import java.util.Locale;
@Named("idiomaBean")
@SessionScoped
public class IdiomaBean implements Serializable {
    private String idioma = "es"; // Idioma por defecto

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public Locale getLocale() {
        return new Locale(idioma);
    }

    public void cambiarIdioma(String nuevoIdioma) {
        this.idioma = nuevoIdioma;
        FacesContext.getCurrentInstance()
                .getViewRoot()
                .setLocale(new Locale(nuevoIdioma));
    }

}

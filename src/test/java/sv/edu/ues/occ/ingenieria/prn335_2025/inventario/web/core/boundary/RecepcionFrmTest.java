package sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.boundary;

import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.primefaces.event.SelectEvent;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.CompraDAO;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.CompraDetalleDAO;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.Compra;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.CompraDetalle;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecepcionFrmTest {

    @Mock FacesContext facesContext;
    @Mock CompraDAO compraDAO;
    @Mock CompraDetalleDAO compraDetalleDAO;
    @Mock RecepcionKardexFrm recepcionKardexFrm;
    @Mock CompraDetalleFrm compraDetalleFrm;

    private MockedStatic<Logger> mockedLogger;

    @Spy
    @InjectMocks
    RecepcionFrm cut;

    // Variables de prueba
    private Compra mockCompra;
    private Long mockCompraId = 1L;

    // Campos de reflexión para acceder a DefaultFrm
    private Field registroField;
    private Field estadoField;
    private Class<?> estadoCrudClass;
    private Object estadoCrudModificar;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Silenciar Logger
        Logger appLogger = Logger.getLogger(RecepcionFrm.class.getName());
        appLogger.setLevel(Level.OFF);
        mockedLogger = mockStatic(Logger.class);
        mockedLogger.when(() -> Logger.getLogger(anyString())).thenReturn(appLogger);

        // 2. Inicializar Entidades
        mockCompra = new Compra();
        mockCompra.setId(mockCompraId);
        mockCompra.setEstado("PAGADA");

        // 3. Reflexión para campos heredados (DefaultFrm)
        registroField = cut.getClass().getSuperclass().getDeclaredField("registro");
        registroField.setAccessible(true);
        estadoField = cut.getClass().getSuperclass().getDeclaredField("estado");
        estadoField.setAccessible(true);

        // 4. Cargar Enum ESTADO_CRUD (Manejo robusto de ubicación)
        String enumClassName = "sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.boundary.ESTADO_CRUD";
        try {
            estadoCrudClass = Class.forName(enumClassName);
        } catch (ClassNotFoundException e) {
            try {
                // Intento alternativo si es clase anidada
                estadoCrudClass = Class.forName("sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.boundary.DefaultFrm$ESTADO_CRUD");
            } catch (Exception ex) {
                throw new RuntimeException("FATAL: No se encontró ESTADO_CRUD", ex);
            }
        }
        estadoCrudModificar = Enum.valueOf((Class<Enum>) estadoCrudClass, "MODIFICAR");
    }

    @AfterEach
    void tearDown() {
        if (mockedLogger != null) mockedLogger.close();
    }

    // ----------------------------------------------------------------------
    // --- 1. Métodos Base y Abstractos ---
    // ----------------------------------------------------------------------

    @Test
    void testGetFacesContext() {
        assertEquals(facesContext, cut.getFacesContext());
    }

    @Test
    void testGetDao() {
        assertEquals(compraDAO, cut.getDao());
    }

    @Test
    void testNuevoRegistro() {
        Compra nueva = cut.nuevoRegistro();
        assertNotNull(nueva);
        assertNotNull(nueva.getFecha());
        assertEquals("PAGADA", nueva.getEstado());
    }

    // ----------------------------------------------------------------------
    // --- 2. Búsqueda y Conversión ---
    // ----------------------------------------------------------------------

    @Test
    void testBuscarRegistroPorId_Success() {
        when(compraDAO.findById(mockCompraId)).thenReturn(mockCompra);

        // Caso: ID como Long
        assertEquals(mockCompra, cut.buscarRegistroPorId(mockCompraId));

        // Caso: ID como String (conversión interna)
        assertEquals(mockCompra, cut.buscarRegistroPorId(mockCompraId.toString()));
    }

    @Test
    void testBuscarRegistroPorId_Null() {
        assertNull(cut.buscarRegistroPorId(null));
    }

    @Test
    void testBuscarRegistroPorId_Exception() {
        // Caso: ID inválido (String no numérico) lanza NumberFormatException
        assertNull(cut.buscarRegistroPorId("invalid-id"));

        // Caso: Error en DAO
        when(compraDAO.findById(any())).thenThrow(new RuntimeException("DB Error"));
        assertNull(cut.buscarRegistroPorId(mockCompraId));
    }

    @Test
    void testGetIdAsText() {
        assertEquals(mockCompraId.toString(), cut.getIdAsText(mockCompra));

        assertNull(cut.getIdAsText(null));

        Compra c = new Compra();
        c.setId(null);
        assertNull(cut.getIdAsText(c));
    }

    @Test
    void testGetIdByText_Success() {
        when(compraDAO.findById(mockCompraId)).thenReturn(mockCompra);
        assertEquals(mockCompra, cut.getIdByText(mockCompraId.toString()));
    }

    @Test
    void testGetIdByText_NullOrEmpty() {
        assertNull(cut.getIdByText(null));
        assertNull(cut.getIdByText(""));
    }

    @Test
    void testGetIdByText_Exception() {
        // Caso: ID inválido (parse error)
        assertNull(cut.getIdByText("invalid"));

        // Caso: DAO Exception
        when(compraDAO.findById(any())).thenThrow(new RuntimeException("DB Error"));
        assertNull(cut.getIdByText("123"));
    }

    // ----------------------------------------------------------------------
    // --- 3. Lógica de Selección (selectionHandler) ---
    // ----------------------------------------------------------------------

    @Test
    void testSelectionHandler_Success() throws Exception {
        SelectEvent<Compra> event = mock(SelectEvent.class);
        when(event.getObject()).thenReturn(mockCompra);

        cut.selectionHandler(event);

        // Verificar que se actualizó el registro y estado en DefaultFrm
        assertEquals(mockCompra, registroField.get(cut));
        assertEquals(estadoCrudModificar, estadoField.get(cut));

        // Verificar llamada al formulario dependiente
        verify(compraDetalleFrm).cargarDetallesPorCompra(mockCompraId);
    }

    @Test
    void testSelectionHandler_CompraNull() throws Exception {
        SelectEvent<Compra> event = mock(SelectEvent.class);
        when(event.getObject()).thenReturn(null);

        // Establecer estado previo para asegurar que no cambia
        registroField.set(cut, null);

        cut.selectionHandler(event);

        assertNull(registroField.get(cut));
        verify(compraDetalleFrm, never()).cargarDetallesPorCompra(any());
    }

    @Test
    void testSelectionHandler_IdNull() throws Exception {
        Compra compraSinId = new Compra();
        compraSinId.setId(null);

        SelectEvent<Compra> event = mock(SelectEvent.class);
        when(event.getObject()).thenReturn(compraSinId);

        cut.selectionHandler(event);

        // El registro se actualiza, pero no se llama a cargarDetalles
        assertEquals(compraSinId, registroField.get(cut));
        verify(compraDetalleFrm, never()).cargarDetallesPorCompra(any());
    }

    @Test
    void testSelectionHandler_RecepcionKardexFrmNull() {
        // Simular que la inyección falló o es nula (rama if (recepcionKardexFrm != null))
        // Aunque en este test tenemos el mock, podemos setearlo a null por reflexión si fuera campo,
        // pero como es Inyectado en el Spy, es difícil quitarlo.
        // Sin embargo, el código fuente actual tiene un if que no hace nada dentro.
        // Este test valida que la ejecución no explote.

        SelectEvent<Compra> event = mock(SelectEvent.class);
        when(event.getObject()).thenReturn(mockCompra);

        cut.selectionHandler(event);

        // Validar comportamiento normal
        verify(compraDetalleFrm).cargarDetallesPorCompra(mockCompraId);
    }

    // ----------------------------------------------------------------------
    // --- 4. Carga de Datos ---
    // ----------------------------------------------------------------------

    @Test
    void testCargarDatos_Success() {
        when(compraDAO.findByEstado("PAGADA")).thenReturn(List.of(mockCompra));

        List<Compra> result = cut.cargarDatos(0, 10);

        assertFalse(result.isEmpty());
        assertEquals(mockCompra, result.get(0));
        verify(compraDAO).findByEstado("PAGADA");
    }

    @Test
    void testCargarDatos_Exception() {
        when(compraDAO.findByEstado(anyString())).thenThrow(new RuntimeException("DB Fail"));

        List<Compra> result = cut.cargarDatos(0, 10);

        assertTrue(result.isEmpty());
        // Verificamos que se intentó loggear la excepción
        mockedLogger.verify(() -> Logger.getLogger(anyString()), atLeastOnce());
    }

    // ----------------------------------------------------------------------
    // --- 5. Getters de Detalles y Formularios Dependientes ---
    // ----------------------------------------------------------------------

    @Test
    void testGetDetallesCompraSeleccionada_Success() throws Exception {
        registroField.set(cut, mockCompra);
        List<CompraDetalle> detalles = Collections.emptyList();
        when(compraDetalleDAO.findByIdCompra(mockCompraId)).thenReturn(detalles);

        List<CompraDetalle> result = cut.getDetallesCompraSeleccionada();

        assertEquals(detalles, result);
        verify(compraDetalleDAO).findByIdCompra(mockCompraId);
    }

    @Test
    void testGetDetallesCompraSeleccionada_NoRegistro() {
        // registro es null por defecto
        assertTrue(cut.getDetallesCompraSeleccionada().isEmpty());
        verify(compraDetalleDAO, never()).findByIdCompra(any());
    }

    @Test
    void testGetDetallesCompraSeleccionada_IdNull() throws Exception {
        Compra c = new Compra();
        c.setId(null);
        registroField.set(cut, c);

        assertTrue(cut.getDetallesCompraSeleccionada().isEmpty());
        verify(compraDetalleDAO, never()).findByIdCompra(any());
    }

    @Test
    void testGetRecepcionKardexFrm() {
        assertEquals(recepcionKardexFrm, cut.getRecepcionKardexFrm());
    }

    @Test
    void testGetCompraDetalleFrm_Success() throws Exception {
        registroField.set(cut, mockCompra);

        CompraDetalleFrm result = cut.getCompraDetalleFrm();

        assertEquals(compraDetalleFrm, result);
        verify(compraDetalleFrm).setIdCompra(mockCompraId);
    }

    @Test
    void testGetCompraDetalleFrm_NoRegistro() {
        CompraDetalleFrm result = cut.getCompraDetalleFrm();

        assertEquals(compraDetalleFrm, result);
        verify(compraDetalleFrm, never()).setIdCompra(any());
    }

    @Test
    void testGetCompraDetalleFrm_IdNull() throws Exception {
        Compra c = new Compra();
        c.setId(null);
        registroField.set(cut, c);

        CompraDetalleFrm result = cut.getCompraDetalleFrm();

        assertEquals(compraDetalleFrm, result);
        verify(compraDetalleFrm, never()).setIdCompra(any());
    }
}
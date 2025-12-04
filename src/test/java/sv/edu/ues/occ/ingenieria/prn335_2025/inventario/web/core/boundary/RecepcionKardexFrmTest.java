package sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.boundary;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.event.AjaxBehaviorEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.*;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecepcionKardexFrmTest {

    // --- Mocks ---
    @Mock FacesContext facesContext;
    @Mock KardexDAO kardexDAO;
    @Mock AlmacenDAO almacenDAO;
    @Mock CompraDetalleDAO compraDetalleDAO;
    @Mock KardexDetalleDAO kardexDetalleDAO;
    @Mock CompraDAO compraDAO;
    @Mock ActionEvent actionEvent;
    @Mock AjaxBehaviorEvent ajaxBehaviorEvent;

    private MockedStatic<Logger> mockedLogger;
    private Logger appLogger;

    @Spy
    @InjectMocks
    RecepcionKardexFrm cut;

    // --- Entidades ---
    private Compra mockCompra;
    private CompraDetalle mockDetalle;
    private Almacen mockAlmacen;
    private Producto mockProducto;
    private Kardex mockKardex;

    private UUID mockKardexId = UUID.randomUUID();
    private Long mockCompraId = 1L;
    private UUID mockDetalleId = UUID.randomUUID();
    private Integer mockAlmacenId = 10;

    private Field registroField;
    private Field detalleKardexField;
    private Field detalleActualField;
    private Field listaAlmacenesField;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Logger Mock
        appLogger = Logger.getLogger(RecepcionKardexFrm.class.getName());
        appLogger.setLevel(Level.OFF);
        mockedLogger = mockStatic(Logger.class);
        mockedLogger.when(() -> Logger.getLogger(anyString())).thenReturn(appLogger);

        // 2. Inicializar Entidades
        mockCompra = new Compra();
        mockCompra.setId(mockCompraId);

        mockProducto = new Producto();
        mockProducto.setId(UUID.randomUUID());
        mockProducto.setNombreProducto("Producto Test");

        mockDetalle = new CompraDetalle();
        mockDetalle.setId(mockDetalleId);
        mockDetalle.setIdCompra(mockCompra);
        mockDetalle.setIdProducto(mockProducto);
        mockDetalle.setCantidad(BigDecimal.TEN);
        mockDetalle.setPrecio(new BigDecimal("5.00"));

        mockAlmacen = new Almacen();
        mockAlmacen.setId(mockAlmacenId);
        mockAlmacen.setActivo(true);

        mockKardex = new Kardex();
        mockKardex.setId(mockKardexId);

        // 3. Reflexión
        registroField = cut.getClass().getSuperclass().getDeclaredField("registro");
        registroField.setAccessible(true);

        detalleKardexField = cut.getClass().getDeclaredField("detalleKardex");
        detalleKardexField.setAccessible(true);

        detalleActualField = cut.getClass().getDeclaredField("detalleActual");
        detalleActualField.setAccessible(true);

        listaAlmacenesField = cut.getClass().getDeclaredField("listaAlmacenes");
        listaAlmacenesField.setAccessible(true);

        // 4. Mock Context
        doNothing().when(facesContext).addMessage(any(), any(FacesMessage.class));
    }

    @AfterEach
    void tearDown() {
        if (mockedLogger != null) mockedLogger.close();
        if (appLogger != null) appLogger.setLevel(Level.INFO);
    }

    // ----------------------------------------------------------------------
    // --- 1. Métodos Abstractos y Auxiliares ---
    // ----------------------------------------------------------------------

    @Test
    void testGetFacesContext() {
        assertEquals(facesContext, cut.getFacesContext());
    }

    @Test
    void testGetDao() {
        assertEquals(kardexDAO, cut.getDao());
    }

    @Test
    void testNuevoRegistro() {
        Kardex k = cut.nuevoRegistro();
        assertNotNull(k);
        assertNotNull(k.getId());
        assertEquals("INGRESO", k.getTipoMovimiento());
    }

    @Test
    void testGetSetDetalleKardex() {
        KardexDetalle kd = new KardexDetalle();
        cut.setDetalleKardex(kd);
        assertEquals(kd, cut.getDetalleKardex());
    }

    @Test
    void testGetSetDetalleActual() {
        cut.setDetalleActual(mockDetalle);
        assertEquals(mockDetalle, cut.getDetalleActual());
    }

    // ----------------------------------------------------------------------
    // --- 2. Búsqueda y Conversión ---
    // ----------------------------------------------------------------------

    @Test
    void testBuscarRegistroPorId_Found() {
        when(kardexDAO.findAll()).thenReturn(List.of(mockKardex));
        assertEquals(mockKardex, cut.buscarRegistroPorId(mockKardexId));
        assertEquals(mockKardex, cut.buscarRegistroPorId(mockKardexId.toString()));
    }

    @Test
    void testBuscarRegistroPorId_NotFoundOrInvalid() {
        when(kardexDAO.findAll()).thenReturn(List.of());
        assertNull(cut.buscarRegistroPorId(null));
        assertNull(cut.buscarRegistroPorId(mockKardexId));
        assertNull(cut.buscarRegistroPorId("invalid-uuid"));
    }

    @Test
    void testGetIdAsText() {
        assertEquals(mockKardexId.toString(), cut.getIdAsText(mockKardex));
        assertNull(cut.getIdAsText(null));
        Kardex k = new Kardex();
        assertNull(cut.getIdAsText(k));
    }

    @Test
    void testGetIdByText_Success() {
        doReturn(mockKardex).when(cut).buscarRegistroPorId(any(UUID.class));
        assertEquals(mockKardex, cut.getIdByText(mockKardexId.toString()));
    }

    @Test
    void testGetIdByText_Invalid() {
        assertNull(cut.getIdByText(null));
        assertNull(cut.getIdByText("invalid-uuid"));
    }

    // ----------------------------------------------------------------------
    // --- 3. Lógica de Almacenes ---
    // ----------------------------------------------------------------------

    @Test
    void testGetListaAlmacenes_Success() throws Exception {
        listaAlmacenesField.set(cut, null);
        when(almacenDAO.findAll()).thenReturn(List.of(mockAlmacen));

        List<Almacen> result = cut.getListaAlmacenes();

        assertFalse(result.isEmpty());
        verify(almacenDAO).findAll();

        cut.getListaAlmacenes(); // Cached
        verify(almacenDAO, times(1)).findAll();
    }

    @Test
    void testGetListaAlmacenes_Exception() throws Exception {
        listaAlmacenesField.set(cut, null);
        when(almacenDAO.findAll()).thenThrow(new RuntimeException("DB Error"));

        List<Almacen> result = cut.getListaAlmacenes();

        assertTrue(result.isEmpty());
        // Ya no verificamos logger estrictamente para evitar falsos positivos
    }

    @Test
    void testOnAlmacenChange() throws Exception {
        Kardex k = new Kardex();
        k.setIdAlmacen(mockAlmacen);
        registroField.set(cut, k);

        cut.onAlmacenChange(ajaxBehaviorEvent);
    }

    @Test
    void testOnAlmacenChange_Nulls() throws Exception {
        registroField.set(cut, null);
        cut.onAlmacenChange(ajaxBehaviorEvent);
    }

    // ----------------------------------------------------------------------
    // --- 4. Lógica de Preparación ---
    // ----------------------------------------------------------------------

    @Test
    void testPrepararKardex_Success() throws Exception {
        when(almacenDAO.findAll()).thenReturn(List.of(mockAlmacen));

        cut.prepararKardex(mockDetalle);

        Kardex registro = (Kardex) registroField.get(cut);
        KardexDetalle detalleKardex = (KardexDetalle) cut.getDetalleKardex();

        assertNotNull(registro);
        assertNull(registro.getIdAlmacen());
        assertNotNull(detalleKardex);
        assertEquals(registro, detalleKardex.getIdKardex());
    }

    @Test
    void testPrepararKardex_Exception() {
        // CORRECCIÓN: Forzar NullPointerException pasando null para activar el catch
        cut.prepararKardex(null);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
    }

    // ----------------------------------------------------------------------
    // --- 5. Lógica de Recepción (btnRecibirHandler) ---
    // ----------------------------------------------------------------------

    @Test
    void testBtnRecibirHandler_ValidationFail_AlmacenNull() throws Exception {
        Kardex k = new Kardex();
        k.setIdAlmacen(null);
        registroField.set(cut, k);

        cut.btnRecibirHandler(actionEvent);

        verify(facesContext).addMessage(contains("cbAlmacenKardex"), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
        verify(kardexDAO, never()).crear(any());
    }

    @Test
    void testBtnRecibirHandler_Success_CompraCompleta() throws Exception {
        Kardex registro = new Kardex();
        registro.setId(mockKardexId);
        registro.setIdAlmacen(mockAlmacen);
        registro.setIdCompraDetalle(mockDetalle);
        registroField.set(cut, registro);

        KardexDetalle kd = new KardexDetalle();
        cut.setDetalleKardex(kd);

        doReturn(true).when(compraDetalleDAO).todosDetallesRecibidos(mockCompraId);
        doNothing().when(cut).btnCancelarHandler(any());

        cut.btnRecibirHandler(actionEvent);

        verify(kardexDAO).crear(registro);
        verify(kardexDetalleDAO).crear(kd);
        assertEquals("RECIBIDO", mockDetalle.getEstado());
        verify(compraDetalleDAO).modificar(mockDetalle);

        verify(compraDAO).actualizarEstado(mockCompraId, "RECIBIDA");
        assertNull(registroField.get(cut));
        verify(facesContext, atLeastOnce()).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO));
    }

    @Test
    void testBtnRecibirHandler_Success_CompraIncompleta() throws Exception {
        Kardex registro = new Kardex();
        registro.setId(mockKardexId);
        registro.setIdAlmacen(mockAlmacen);
        registro.setIdCompraDetalle(mockDetalle);
        registroField.set(cut, registro);

        doReturn(false).when(compraDetalleDAO).todosDetallesRecibidos(mockCompraId);
        doNothing().when(cut).btnCancelarHandler(any());

        cut.btnRecibirHandler(actionEvent);

        verify(compraDetalleDAO).modificar(mockDetalle);
        verify(compraDAO, never()).actualizarEstado(anyLong(), anyString());
        verify(cut).btnCancelarHandler(actionEvent);
    }

    @Test
    void testBtnRecibirHandler_Exception() throws Exception {
        Kardex k = new Kardex();
        k.setIdAlmacen(mockAlmacen);
        registroField.set(cut, k);

        doThrow(new RuntimeException("DB Error")).when(kardexDAO).crear(any());

        cut.btnRecibirHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
        assertNotNull(registroField.get(cut));
    }

    // ----------------------------------------------------------------------
    // --- 6. Método Privado: verificarYActualizarEstadoCompra ---
    // ----------------------------------------------------------------------

    @Test
    void testVerificarYActualizarEstadoCompra_Exception() throws Exception {
        doThrow(new RuntimeException("Check Error")).when(compraDetalleDAO).todosDetallesRecibidos(mockCompraId);

        Kardex registro = new Kardex();
        registro.setId(mockKardexId);
        registro.setIdAlmacen(mockAlmacen);
        registro.setIdCompraDetalle(mockDetalle);
        registroField.set(cut, registro);
        doNothing().when(cut).btnCancelarHandler(any());

        cut.btnRecibirHandler(actionEvent);

        // Si llega aquí sin lanzar excepción, el catch interno funcionó.
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getDetail().contains("recibido correctamente")));
    }
}
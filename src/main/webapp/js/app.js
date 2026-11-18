let pedidosCache = [];
let filtroActual = 'TODOS';

document.addEventListener('DOMContentLoaded', () => {
    cargarDatos();

    document.getElementById('form-pedido').addEventListener('submit', async (e) => {
        e.preventDefault();
        const cliente = document.getElementById('input-cliente').value.trim();
        const producto = document.getElementById('input-producto').value;
        const precio = document.getElementById('input-precio').value;

        if (!cliente) return;

        const params = new URLSearchParams();
        params.append('cliente', cliente);
        params.append('producto', producto);
        params.append('precio', precio);

        await fetch('api/pedidos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        });

        document.getElementById('input-cliente').value = '';
        mostrarToast(`Pedido de ${cliente} (${producto}) guardado en el servidor.`);
        cargarDatos();
    });
});

function seleccionarProducto(cardElement, nombre, precio) {
    document.querySelectorAll('.product-card').forEach(c => c.classList.remove('selected'));
    cardElement.classList.add('selected');
    document.getElementById('input-producto').value = nombre;
    document.getElementById('input-precio').value = precio.toFixed(2);
    document.getElementById('input-cliente').focus();
}

async function cargarDatos() {
    try {
        const res = await fetch('api/pedidos');
        const data = await res.json();

        document.getElementById('env-app').textContent = data.config.appEnv;
        document.getElementById('env-url').textContent = data.config.dbUrl;
        document.getElementById('env-user').textContent = data.config.dbUser;
        document.getElementById('env-pass').textContent = data.config.dbPass;
        document.getElementById('java-version').textContent = 'Java ' + data.config.javaVersion;
        document.getElementById('badge-env').textContent = `☁️ ${data.config.appEnv}`;
        document.getElementById('badge-port').textContent = `🟢 Puerto ${data.config.port}`;

        pedidosCache = data.pedidos || [];
        renderizarPedidos();
    } catch (err) {
        console.error('Error cargando datos del servidor:', err);
    }
}

function filtrarPedidos(estado, btn) {
    filtroActual = estado;
    btn.parentElement.querySelectorAll('button').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    renderizarPedidos();
}

function renderizarPedidos() {
    const contenedor = document.getElementById('lista-pedidos');
    contenedor.innerHTML = '';

    let total = 0;
    pedidosCache.forEach(p => total += Number(p.precio || 0));
    document.getElementById('total-ventas').textContent = `S/ ${total.toFixed(2)}`;

    const filtrados = pedidosCache.filter(p => filtroActual === 'TODOS' || p.estado === filtroActual);

    if (filtrados.length === 0) {
        contenedor.innerHTML = `<div class="text-center text-muted py-4">No hay pedidos en esta categoría.</div>`;
        return;
    }

    filtrados.forEach(p => {
        const badgeColor = p.estado === 'Listo' ? 'bg-success' : (p.estado === 'Entregado' ? 'bg-secondary' : 'bg-warning text-dark');
        const item = document.createElement('div');
        item.className = 'card border shadow-sm pedido-item';
        item.innerHTML = `
            <div class="card-body py-2 px-3 d-flex justify-content-between align-items-center">
                <div>
                    <div class="d-flex align-items-center gap-2">
                        <span class="fw-bold text-coffee">#${p.id}</span>
                        <span class="fw-semibold">${p.producto}</span>
                        <span class="badge ${badgeColor}">${p.estado}</span>
                    </div>
                    <div class="small text-muted">
                        👤 ${p.cliente} · 🕒 ${p.hora} · <strong>S/ ${Number(p.precio).toFixed(2)}</strong>
                    </div>
                </div>
                <div class="d-flex gap-1">
                    <button class="btn btn-sm btn-outline-success fw-bold" title="Cambiar estado" onclick="cambiarEstado(${p.id})">
                        ✓
                    </button>
                    <button class="btn btn-sm btn-outline-danger" title="Eliminar" onclick="eliminarPedido(${p.id})">
                        🗑️
                    </button>
                </div>
            </div>
        `;
        contenedor.appendChild(item);
    });
}

async function cambiarEstado(id) {
    const params = new URLSearchParams({ accion: 'cambiarEstado', id: id });
    await fetch('api/pedidos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
    });
    cargarDatos();
}

async function eliminarPedido(id) {
    const params = new URLSearchParams({ accion: 'eliminar', id: id });
    await fetch('api/pedidos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
    });
    mostrarToast(`Pedido #${id} eliminado.`);
    cargarDatos();
}

function mostrarToast(mensaje) {
    document.getElementById('toast-mensaje').textContent = mensaje;
    const toastEl = document.getElementById('toast-notificacion');
    const toast = new bootstrap.Toast(toastEl);
    toast.show();
}
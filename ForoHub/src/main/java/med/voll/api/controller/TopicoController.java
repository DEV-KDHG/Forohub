package med.voll.api.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import med.voll.api.domain.topico.*;
import med.voll.api.domain.usuarios.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/topicos")
public class TopicoController {

    @Autowired
    private TopicoRepository topicoRepository;

    @PostMapping
    public ResponseEntity<DatosRespuestaTopico> registrarTopico(@RequestBody @Valid DatosRegistroTopico datosRegistroTopico,
                                                                UriComponentsBuilder uriComponentsBuilder) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = (Usuario) authentication.getPrincipal();

        // Verificar si ya existe un tópico con el mismo autor, título, mensaje y que esté activo
        Optional<Topico> topicoExistente = topicoRepository.findByAutorAndTituloAndMensajeAndActivoTrue(
                usuario.getLogin(), datosRegistroTopico.titulo(), datosRegistroTopico.mensaje());

        if (topicoExistente.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un tópico activo con el mismo autor, título y mensaje");
        }

        // Crear un nuevo Topico con el autor del usuario autenticado
        Topico topico = new Topico(datosRegistroTopico.titulo(),
                usuario.getLogin(), // Establecer el autor aquí
                datosRegistroTopico.mensaje(),
                datosRegistroTopico.curso(),
                LocalDateTime.now());

        topico = topicoRepository.save(topico);
        DatosRespuestaTopico datosRespuestaTopico = new DatosRespuestaTopico(topico.getId(), topico.getTitulo(),
                topico.getAutor(), topico.getMensaje(), topico.getCurso(), topico.getFechaCreacion());

        URI url = uriComponentsBuilder.path("/topicos/{id}").buildAndExpand(topico.getId()).toUri();
        return ResponseEntity.created(url).body(datosRespuestaTopico);
    }

    @GetMapping
    public ResponseEntity<Page<DatosListadoTopico>> listadoTopicos(@PageableDefault(size = 10) Pageable paginacion) {
        return ResponseEntity.ok(topicoRepository.findByActivoTrue(paginacion).map(DatosListadoTopico::new));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatosRespuestaTopico> retornaDatosTopico(@PathVariable Long id) {
        Topico topico = topicoRepository.getReferenceById(id);
        var datosTopico = new DatosRespuestaTopico(topico.getId(), topico.getTitulo(), topico.getAutor(), topico.getMensaje(), topico.getCurso(), topico.getFechaCreacion());
        return ResponseEntity.ok(datosTopico);
    }

    @GetMapping("/{id}/url")
    public ResponseEntity<String> obtenerUrlTopico(@PathVariable Long id, UriComponentsBuilder uriComponentsBuilder) {
        Topico topico = topicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tópico no encontrado"));

        URI url = uriComponentsBuilder.path("/topicos/{id}").buildAndExpand(topico.getId()).toUri();
        return ResponseEntity.ok(url.toString());
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> actualizarTopico(@PathVariable Long id, @RequestBody @Valid DatosActualizarTopico datosActualizarTopico) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = (Usuario) authentication.getPrincipal();
        Topico topico = topicoRepository.findByIdAndAutor(id, usuario.getLogin())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este tópico"));

        // Verificar si el tópico está activo
        if (!topico.isActivo()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe ese tópico");
        }

        // Actualizar los datos del tópico
        if (datosActualizarTopico.titulo() != null) {
            topico.setTitulo(datosActualizarTopico.titulo());
        }
        if (datosActualizarTopico.mensaje() != null) {
            topico.setMensaje(datosActualizarTopico.mensaje());
        }

        topicoRepository.save(topico);

        DatosRespuestaTopico datosRespuestaTopico = new DatosRespuestaTopico(
                topico.getId(), topico.getTitulo(), topico.getAutor(), topico.getMensaje(), topico.getCurso(), topico.getFechaCreacion());

        return ResponseEntity.ok(datosRespuestaTopico);
    }


    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminarTopico(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = (Usuario) authentication.getPrincipal();
        Topico topico = topicoRepository.findByIdAndAutor(id, usuario.getLogin())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar este tópico"));
        topico.desactivarTopico();
        return ResponseEntity.noContent().build();
    }
}

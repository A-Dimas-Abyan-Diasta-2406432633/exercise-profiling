package com.advpro.profiling.tutorial.service;

import com.advpro.profiling.tutorial.model.Course;
import com.advpro.profiling.tutorial.model.Student;
import com.advpro.profiling.tutorial.model.StudentCourse;
import com.advpro.profiling.tutorial.repository.StudentCourseRepository;
import com.advpro.profiling.tutorial.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class StudentServiceTest {

    @Test
    void getAllStudentsWithCoursesUsesSingleRepositoryCall() {
        Student student = new Student("S1", "Dimas", "CS", 3.9);
        Course course = new Course("C1", "AdvPro", "Profiling");
        StudentCourse studentCourse = new StudentCourse(student, course);
        AtomicBoolean methodCalled = new AtomicBoolean(false);

        StudentCourseRepository studentCourseRepository = proxyRepository(
                StudentCourseRepository.class,
                Map.of("findAllWithStudentAndCourse", List.of(studentCourse)),
                "findAllWithStudentAndCourse",
                methodCalled
        );

        StudentService studentService = new StudentService();
        ReflectionTestUtils.setField(studentService, "studentCourseRepository", studentCourseRepository);

        List<StudentCourse> result = studentService.getAllStudentsWithCourses();

        assertThat(result).containsExactly(studentCourse);
        assertThat(methodCalled.get()).isTrue();
    }

    @Test
    void findStudentWithHighestGpaDelegatesToRepositoryOrdering() {
        Student highestGpaStudent = new Student("S2", "Alya", "CS", 4.0);
        AtomicBoolean methodCalled = new AtomicBoolean(false);

        StudentRepository studentRepository = proxyRepository(
                StudentRepository.class,
                Map.of("findFirstByOrderByGpaDesc", java.util.Optional.of(highestGpaStudent)),
                "findFirstByOrderByGpaDesc",
                methodCalled
        );

        StudentService studentService = new StudentService();
        ReflectionTestUtils.setField(studentService, "studentRepository", studentRepository);

        assertThat(studentService.findStudentWithHighestGpa()).contains(highestGpaStudent);
        assertThat(methodCalled.get()).isTrue();
    }

    @Test
    void joinStudentNamesUsesDelimiterWithoutTrailingComma() {
        AtomicBoolean methodCalled = new AtomicBoolean(false);

        StudentRepository studentRepository = proxyRepository(
                StudentRepository.class,
                Map.of("findAllNames", List.of("Dimas", "Alya", "Bima")),
                "findAllNames",
                methodCalled
        );

        StudentService studentService = new StudentService();
        ReflectionTestUtils.setField(studentService, "studentRepository", studentRepository);

        assertThat(studentService.joinStudentNames()).isEqualTo("Dimas, Alya, Bima");
        assertThat(methodCalled.get()).isTrue();
    }

    @Test
    void joinStudentNamesReturnsEmptyStringWhenNoStudentExists() {
        AtomicBoolean methodCalled = new AtomicBoolean(false);

        StudentRepository studentRepository = proxyRepository(
                StudentRepository.class,
                Map.of("findAllNames", List.of()),
                "findAllNames",
                methodCalled
        );

        StudentService studentService = new StudentService();
        ReflectionTestUtils.setField(studentService, "studentRepository", studentRepository);

        assertThat(studentService.joinStudentNames()).isEmpty();
        assertThat(methodCalled.get()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxyRepository(
            Class<T> repositoryType,
            Map<String, Object> methodResults,
            String expectedMethodName,
            AtomicBoolean methodCalled
    ) {
        Map<String, Object> responses = new HashMap<>(methodResults);
        return (T) Proxy.newProxyInstance(
                repositoryType.getClassLoader(),
                new Class[]{repositoryType},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> repositoryType.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }

                    if (method.getName().equals(expectedMethodName)) {
                        methodCalled.set(true);
                    }

                    if (responses.containsKey(method.getName())) {
                        return responses.get(method.getName());
                    }

                    throw new UnsupportedOperationException("Unexpected repository call: " + method.getName());
                }
        );
    }
}

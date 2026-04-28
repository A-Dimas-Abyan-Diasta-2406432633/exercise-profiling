package com.advpro.profiling.tutorial.service;

import com.advpro.profiling.tutorial.model.Student;
import com.advpro.profiling.tutorial.model.StudentCourse;
import com.advpro.profiling.tutorial.repository.StudentCourseRepository;
import com.advpro.profiling.tutorial.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * @author muhammad.khadafi
 */
@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentCourseRepository studentCourseRepository;

    public List<StudentCourse> getAllStudentsWithCourses() {
        return studentCourseRepository.findAllWithStudentAndCourse();
    }

    public Optional<Student> findStudentWithHighestGpa() {
        return studentRepository.findFirstByOrderByGpaDesc();
    }

    public String joinStudentNames() {
        List<String> studentNames = studentRepository.findAllNames();
        StringJoiner joiner = new StringJoiner(", ");
        for (String studentName : studentNames) {
            joiner.add(studentName);
        }
        return joiner.toString();
    }
}
